(ns clean-chat.stage06-sql.sql-queries
  (:require [honey.sql :as hsql]
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

(def user-domain->sql-keys {:username :name :room-name :room_name})
(def user-sql->domain-keys {:name           :username
                            :room_name      :room-name
                            :user/uuid      :uuid
                            :user/name      :username
                            :user/room_name :room-name})
(def select-user-sql {:select [:*] :from :user})

(defn get-user [this user]
  (let [{:keys [name uuid]} (rename-keys user user-domain->sql-keys)]
    (when-some [where (cond
                        uuid [:= :uuid uuid]
                        name [:= :name name])]
      (let [sql (assoc select-user-sql :where where)]
        (-> (sql/query this (hsql/format sql))
            first
            (rename-keys user-sql->domain-keys))))))

(defn insert-user! [this {:keys [uuid] :as user}]
  (let [sql-user (cond-> (rename-keys user user-domain->sql-keys)
                         (nil? uuid)
                         (assoc :uuid (random-uuid)))]
    (sql/insert! this :user sql-user)
    (get-user this sql-user)))

(defn update-user! [this user]
  (let [{:keys [uuid name] :as sql-user} (rename-keys user user-domain->sql-keys)]
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
(def message-domain->sql-keys {:username  :user_name
                               :room-name :room_name})
(def message-sql->domain-keys {:message/uuid      :uuid
                               :message/message   :message
                               :message/nanos     :nanos
                               :message/room_name :room-name
                               :message/user_name :username})
(def select-message-sql {:select [:*] :from :message})

(defn get-message [this {:keys [uuid]}]
  (when uuid
    (let [sql (assoc select-message-sql :where [:= :uuid uuid])]
      (-> (sql/query this (hsql/format sql))
          first
          (rename-keys message-sql->domain-keys)))))

(defn get-messages [this]
  (let [sql select-message-sql]
    (->> (sql/query this (hsql/format sql))
         (map #(rename-keys % message-sql->domain-keys)))))

(defn get-messages-for-room [this room-name]
  (let [sql (assoc select-message-sql
              :where [:= :room_name room-name])]
    (->> (sql/query this (hsql/format sql))
         (map #(rename-keys % message-sql->domain-keys)))))

(defn insert-message! [this {:keys [uuid nanos] :as message}]
  (let [sql-message (cond-> (rename-keys message message-domain->sql-keys)
                            (nil? uuid)
                            (assoc :uuid (random-uuid))
                            (nil? nanos)
                            (assoc :nanos (System/nanoTime)))]
    (sql/insert! this :message sql-message)
    (get-message this sql-message)))

;(-> {:delete [:films :directors],
;     :from [:films],
;     :join [:directors [:= :films.director_id :directors.id]],
;     :where [:<> :kind "musical"]}
;    (sql/format {:pretty true}))

(defn occupied-rooms [this]
  (let [sql (-> select-room-sql
                (dissoc :select)
                (assoc :select-distinct [:room.name]
                       :join [:user [:= :room.name :user.room_name]]
                       :order-by [:room.name]))]
    (->> (sql/query this (hsql/format sql))
         (map #(:room-name (rename-keys % room-sql->domain-keys))))))

(defn all-active-users [this]
  (let [sql (assoc select-user-sql :where [:not= :room_name nil])]
    (->> (sql/query this (hsql/format sql))
         (map #(:username (rename-keys % user-sql->domain-keys))))))

(defn users-in-room [this room-name]
  (let [sql (assoc select-user-sql :where [:= :room_name room-name])]
    (->> (sql/query this (hsql/format sql))
         (map #(:username (rename-keys % user-sql->domain-keys))))))

(defn current-room-name [this username]
  (:room-name (get-user this {:name username})))