(ns clean-chat.stage06-sql.queries-datascript
  (:require
    [datascript.core :as d]))

(def chat-schema
  {:username  {:db/unique :db.unique/identity}
   :room-name {:db/unique :db.unique/identity}
   :user      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}
   :room      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}})

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-active-users-query
  '[:find ?username ?room-name
    :keys username room-name
    :in $
    :where
    [?e :username ?username]
    [?e :room ?r]
    [?r :room-name ?room-name]])

(defn occupied-rooms [db]
  (sort (d/q all-rooms-query db)))

(defn all-active-users [db]
  (->> (d/q all-active-users-query db) (map :username) sort))

(defn users-in-room [db room-name]
  (d/q
    '[:find [?username ...]
      :in $ ?room-name
      :where
      [?room :room-name ?room-name]
      [?client :room ?room]
      [?client :username ?username]]
    db room-name))

(defn current-room-name [db username]
  (some-> db (d/entity [:username username]) :room :room-name))

(defn room [db room-name]
  (d/entity db [:room-name room-name]))

(defn chat-history [db room-name]
  (->> (d/q
         '[:find ?username ?message ?t
           :keys username message t
           :in $ ?room-name
           :where
           [?r :room-name ?room-name]
           [?m :room ?r]
           [?m :user ?u]
           [?u :username ?username]
           [?m :message ?message]
           [?m :nanos ?t]]
         db room-name)
       (sort-by :t)))

;; Functional commands ;;
(defn create-message [db {:keys [username message room-name]}]
  {:pre [username message room-name]}
  (d/db-with db [{:message message
                  :user    {:username username}
                  :room    {:room-name room-name}}]))

(defn join-chat [db {:keys [username]}]
  {:pre [username]}
  (d/db-with db [{:username username}]))

(defn insert-room [db room-name]
  (d/db-with db [{:room-name room-name}]))

(defn enter-room [db {:keys [username room-name]}]
  (d/db-with db [{:username username :room {:room-name room-name}}]))

(defn leave-room [db {:keys [username]}]
  {:pre [username]}
  (d/db-with db [[:db.fn/retractAttribute [:username username] :room]]))

(defn rename-room [db {:keys [old-room-name new-room-name]}]
  (let [id (:db/id (room db old-room-name))]
    (let [tx-data [[:db/add id :room-name new-room-name]]]
      (d/db-with db tx-data))))