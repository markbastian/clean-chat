(ns clean-chat.stage06-sql.chat-impl-sqlite
  (:require
   [clean-chat.stage06-sql.api.reducer :as reducer-api]
   [clean-chat.stage06-sql.chat-api :as chat-api]
   [clean-chat.stage06-sql.domain-sql :as domain-sql]
   [clean-chat.stage06-sql.sql-migrations :as sql-migrations]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [parts.next.jdbc.core :as parts.jdbc]))

(defrecord SqlChat [db])

(extend-type SqlChat
  chat-api/IChatEvents
  (create-message! [{:keys [db]} event]
    (domain-sql/insert-message!
     db
     (select-keys event [:username :room-name :message])))
  (join-chat! [{:keys [db]} {:keys [username]}]
    (println username)
    (domain-sql/upsert-user! db {:username username}))
  (leave-chat! [{:keys [db]} {:keys [username]}]
    (domain-sql/upsert-user! db {:username   username
                                 :room-name nil}))
  (create-room! [{:keys [db]} {:keys [room-name]}]
    (domain-sql/insert-room! db {:room-name room-name}))
  (enter-room! [{:keys [db]} {:keys [username room-name]}]
    (domain-sql/upsert-user! db {:username   username
                                 :room-name room-name}))
  (leave-room! [{:keys [db]} {:keys [username]}]
    (domain-sql/upsert-user! db {:username   username
                                 :room-name nil}))
  (rename-room! [{:keys [db]} {:keys [old-room-name new-room-name]}]
    (when-some [uuid (:uuid (domain-sql/get-room db {:room-name old-room-name}))]
      (domain-sql/update-room! db {:uuid uuid :room-name new-room-name})))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [db]}]
    (domain-sql/occupied-rooms db))
  (all-active-users [{:keys [db]}]
    (domain-sql/all-active-users db))
  (users-in-room [{:keys [db]} room-name]
    (domain-sql/users-in-room db room-name))
  (current-room-name [{:keys [db]} username]
    (domain-sql/current-room-name db username))
  (room [{:keys [db]} room-name]
    (domain-sql/get-room db {:name room-name}))
  (chat-history [{:keys [db]} room-name]
    (domain-sql/get-messages-for-room db room-name))
  reducer-api/IOutbox
  (outbox-write! [{:keys [db]} evt]
    (domain-sql/insert-outbox-event! db evt))
  (outbox-read [{:keys [db]}]
    (domain-sql/get-outbox-events db))
  (outbox-get [{:keys [db]} event]
    (domain-sql/get-outbox-event db event))
  (outbox-delete! [{:keys [db]} event]
    (domain-sql/delete-outbox-event! db event))
  reducer-api/IReducer
  (plan-and-execute! [this command]
    (jdbc/with-transaction [tx (:db this)]
      (let [txconn         (assoc this :db tx)
            planned-events (reducer-api/generate-plan txconn command)]
        (reducer-api/execute-events! txconn planned-events)))))

(defmethod ig/init-key ::sql-chat [_ m]
  (log/debug "Creating SQL Chat")
  (map->SqlChat m))

(def config
  {::parts.jdbc/datasource {:dbtype       "sqlite"
                            :dbname       "chat-state"
                            :foreign_keys "on"}
   ::parts.jdbc/migrations {:db         (ig/ref ::parts.jdbc/datasource)
                            :migrations [sql-migrations/create-room-table-sql
                                         sql-migrations/create-user-table-sql
                                         sql-migrations/create-message-table-sql
                                         sql-migrations/create-outbox-table-sql]}
   ::parts.jdbc/teardown   {:db       (ig/ref ::parts.jdbc/datasource)
                            :commands [sql-migrations/drop-outbox-table-sql
                                       sql-migrations/drop-message-table-sql
                                       sql-migrations/drop-user-table-sql
                                       sql-migrations/drop-room-table-sql]}
   ::sql-chat              {:db (ig/ref ::parts.jdbc/datasource)}})
