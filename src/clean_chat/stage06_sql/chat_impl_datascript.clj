(ns clean-chat.stage06-sql.chat-impl-datascript
  (:require
    [clean-chat.stage06-sql.queries :as queries]
    [datascript.core :as d]
    [clean-chat.stage06-sql.chat-api :as chat-api]))

(defrecord DatascriptChat [db outbox])

(defn update-db [this tx-data]
  (update this :db d/db-with tx-data))

(extend-type DatascriptChat
  chat-api/IChatEvents
  (create-message [this {:keys [username message room-name]}]
    (update-db
      this
      [{:message                message
        :nanos-since-unix-epoch (System/nanoTime)
        :user                   {:username username}
        :room                   {:room-name room-name}}]))
  (join-chat [this {:keys [username]}]
    (update-db this [{:username username}]))
  (leave-chat [this {:keys [username]}]
    (update-db this [[:db.fn/retractAttribute [:username username] :room]]))
  (create-room [this {:keys [room-name]}]
    (update-db this [{:room-name room-name}]))
  (enter-room [this {:keys [username room-name]}]
    (update-db this [{:username username :room {:room-name room-name}}]))
  (leave-room [this {:keys [username]}]
    (update this :db d/db-with [[:db.fn/retractAttribute [:username username] :room]]))
  (rename-room [{:keys [db] :as this} {:keys [old-room-name new-room-name]}]
    (let [id (:db/id (queries/room db old-room-name))]
      (let [tx-data [[:db/add id :room-name new-room-name]]]
        (update-db this tx-data))))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [db]}]
    (queries/occupied-rooms db))
  (all-active-users [{:keys [db]}]
    (queries/all-active-users db))
  (users-in-room [{:keys [db]} room-name]
    (queries/users-in-room db room-name))
  (current-room-name [{:keys [db]} username]
    (some-> db (d/entity [:username username]) :room :room-name))
  (room [{:keys [db]} room-name]
    (d/entity db [:room-name room-name]))
  (chat-history [{:keys [db]} room-name]
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
             [?m :nanos-since-unix-epoch ?t]]
           db room-name)
         (sort-by :t))))