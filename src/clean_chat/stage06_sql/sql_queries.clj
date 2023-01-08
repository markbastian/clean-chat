(ns clean-chat.stage06-sql.sql-queries
  (:require
    [clojure.edn :as edn]
    [honey.sql :as hsql]
    [next.jdbc.sql :as sql]
    [clojure.set :refer [rename-keys]]))

(def room-domain->sql-keys {:room-name :name})
(def room-sql->domain-keys {:name      :room-name
                            :room/uuid :uuid
                            :room/name :room-name})

(def select-room-sql {:select [:*] :from :room})

(defn get-room [this room]
  (let [{:keys [name uuid]} (rename-keys room room-domain->sql-keys)]
    (when-some [where (cond
                        uuid [:= :uuid uuid]
                        name [:= :name name])]
      (let [sql (assoc select-room-sql :where where)]
        (-> (sql/query this (hsql/format sql))
            first
            (rename-keys room-sql->domain-keys))))))

(defn insert-room! [this {:keys [uuid] :as room}]
  (let [sql-room (cond-> (rename-keys room room-domain->sql-keys)
                         (nil? uuid)
                         (assoc :uuid (random-uuid)))]
    (sql/insert! this :room sql-room)
    (get-room this room)))

(defn update-room! [this room]
  (let [{:keys [uuid name] :as sql-room} (rename-keys room room-domain->sql-keys)]
    (cond
      uuid (sql/update! this :room sql-room ["uuid = ?" uuid])
      name (sql/update! this :room sql-room ["name = ?" name]))
    (get-room this sql-room)))

(defn upsert-room! [this room]
  (let [sql-room (rename-keys room room-domain->sql-keys)]
    (if-some [uuid (:uuid (get-room this sql-room))]
      (get-room this {:uuid uuid})
      (insert-room! this sql-room))))

(def user-domain->sql-keys {:username  :name
                            :room-name :room_name
                            :room-uuid :room_uuid})
(def user-sql->domain-keys {:name           :username
                            :room_name      :room-name
                            :user/uuid      :uuid
                            :user/name      :username
                            :user/username  :username
                            :room/room_name :room-name
                            :user/room_name :room-name})
(def select-user-sql {:select [:*] :from :user})

(defn get-user [this user]
  (let [{:keys [name uuid]} (rename-keys user user-domain->sql-keys)]
    (when-some [where (cond
                        uuid [:= :user.uuid uuid]
                        name [:= :user.name name])]
      (let [sql (assoc select-user-sql
                  :select [[:user.uuid :uuid]
                           [:user.name :username]
                           [:room.name :room_name]]
                  :left-join [:room [:= :room.uuid :user.room_uuid]]
                  :where where)]
        (-> (sql/query this (hsql/format sql))
            first
            (rename-keys user-sql->domain-keys))))))

(defn insert-user! [this {:keys [uuid room-name] :as user}]
  (let [room-uuid (when room-name
                    (:uuid (get-room this {:name room-name})))
        sql-user (cond-> (rename-keys user user-domain->sql-keys)
                         (nil? uuid)
                         (assoc :uuid (random-uuid))
                         room-uuid
                         (assoc :room_uuid room-uuid)
                         room-name
                         (dissoc :room_name))]
    (sql/insert! this :user sql-user)
    (get-user this sql-user)))

(defn update-user! [this user]
  (let [{:keys [uuid name room_name] :as sql-user} (rename-keys user user-domain->sql-keys)
        sql-user (cond-> sql-user
                         (contains? sql-user :room_name)
                         (->
                           (dissoc :room_name)
                           (assoc :room_uuid (:uuid (get-room this {:name room_name})))))]
    (cond
      uuid (sql/update! this :user sql-user ["uuid = ?" uuid])
      name (sql/update! this :user sql-user ["name = ?" name]))
    (get-user this sql-user)))

(defn upsert-user! [this user]
  (let [sql-user (rename-keys user user-domain->sql-keys)]
    (if-some [uuid (:uuid (get-user this sql-user))]
      (update-user! this (assoc sql-user :uuid uuid))
      (insert-user! this sql-user))))

;; Messages
(def message-domain->sql-keys {:room-uuid :room_uuid
                               :room-name :room_name})
(def message-sql->domain-keys {:message/uuid    :uuid
                               :message/message :message
                               :message/nanos   :nanos
                               :room/room_name  :room-name
                               :user/user_name  :username})
(def select-message-sql {:select [:message
                                  :message.uuid
                                  [:room.name :room_name]
                                  [:user.name :user_name]
                                  :nanos]
                         :join   [:room [:= :message.room_uuid :room.uuid]
                                  :user [:= :message.user_uuid :user.uuid]]
                         :from   :message})

(defn get-message [this {:keys [uuid]}]
  (when uuid
    (let [sql (assoc select-message-sql :where [:= :message.uuid uuid])]
      (-> (sql/query this (hsql/format sql))
          first
          (rename-keys message-sql->domain-keys)))))

(defn get-messages [this]
  (let [sql select-message-sql]
    (->> (sql/query this (hsql/format sql))
         (map #(rename-keys % message-sql->domain-keys)))))

(defn get-messages-for-room [this room-name]
  (let [sql (assoc select-message-sql :where [:= :room.name room-name])]
    (->> (sql/query this (hsql/format sql))
         (map #(rename-keys % message-sql->domain-keys)))))

(defn insert-message! [this {:keys [uuid nanos] :as message}]
  (let [sql-message (cond-> (rename-keys message message-domain->sql-keys)
                            (nil? uuid)
                            (assoc :uuid (random-uuid))
                            (nil? nanos)
                            (assoc :nanos (System/nanoTime)))
        {:keys [room_name room_uuid username user_uuid] :as sql-message} sql-message
        sql-message (cond-> sql-message
                            (and room_name (nil? room_uuid))
                            (assoc :room_uuid (:uuid (get-room this {:name room_name})))
                            (and username (nil? user_uuid))
                            (assoc :user_uuid (:uuid (get-user this {:name username}))))]
    (sql/insert! this :message (select-keys sql-message [:uuid :message :room_uuid :user_uuid :nanos]))
    (get-message this sql-message)))

(def outbox-sql->domain-keys {:outbox/uuid  :uuid
                              :outbox/nanos :nanos
                              :outbox/event :event})

(def select-outbox-sql {:select [:*] :from :outbox :order-by [:nanos]})

(defn get-outbox-events [this]
  (->> (sql/query this (hsql/format select-outbox-sql))
       (map (comp edn/read-string :outbox/event))))

(defn get-outbox-event [this {:keys [uuid]}]
  (let [sql (hsql/format (assoc select-outbox-sql :where [:= :uuid uuid]))]
    (-> (sql/query this sql)
        first
        :outbox/event
        edn/read-string)))

(defn insert-outbox-event! [this {:keys [uuid nanos] :as evt}]
  (let [{:keys [uuid nanos] :as sql-message} (cond-> evt
                                                     (nil? uuid)
                                                     (assoc :uuid (random-uuid))
                                                     (nil? nanos)
                                                     (assoc :nanos (System/nanoTime)))]
    (sql/insert! this :outbox {:uuid  uuid
                               :nanos nanos
                               :event sql-message})
    (get-outbox-event this sql-message)))

(defn delete-outbox-event! [this {:keys [uuid]}]
  (sql/delete! this :outbox ["uuid = ?" uuid]))

(defn occupied-rooms [this]
  (let [sql (-> select-room-sql
                (dissoc :select)
                (assoc :select-distinct [:room.name]
                       :join [:user [:= :room.uuid :user.room_uuid]]
                       :order-by [:room.name]))]
    (->> (sql/query this (hsql/format sql))
         (map #(:room-name (rename-keys % room-sql->domain-keys))))))

(defn all-active-users [this]
  (let [sql (assoc select-user-sql :where [:not= :room_uuid nil])]
    (->> (sql/query this (hsql/format sql))
         (map #(:username (rename-keys % user-sql->domain-keys))))))

(defn users-in-room [this room-name]
  (let [sql (assoc select-user-sql
              :join [:room [:= :user.room_uuid :room.uuid]]
              :where [:= :room.name room-name])]
    (->> (sql/query this (hsql/format sql))
         (map #(:username (rename-keys % user-sql->domain-keys))))))

(defn current-room-name [this username]
  (:room-name (get-user this {:name username})))