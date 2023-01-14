(ns clean-chat.stage06-sql.chat-impl-sqlite
  (:require [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.queries-sql :as sql-queries]))

(defrecord SqlChat [db])

(extend-type SqlChat
  chat-api/IChatEvents
  (create-message! [{:keys [db]} event]
    (sql-queries/insert-message!
      db
      (select-keys event [:username :room-name :message])))
  (join-chat! [{:keys [db]} {:keys [username]}]
    (sql-queries/upsert-user! db {:username username}))
  (leave-chat! [{:keys [db]} {:keys [username]}]
    (sql-queries/upsert-user! db {:username  username
                                  :room-name nil}))
  (create-room! [{:keys [db]} {:keys [room-name]}]
    (sql-queries/insert-room! db {:room-name room-name}))
  (enter-room! [{:keys [db]} {:keys [username room-name]}]
    (sql-queries/upsert-user! db {:username  username
                                  :room-name room-name}))
  (leave-room! [{:keys [db]} {:keys [username]}]
    (sql-queries/upsert-user! db {:username  username
                                  :room-name nil}))
  (rename-room! [{:keys [db]} {:keys [old-room-name new-room-name]}]
    (if-some [uuid (:uuid (sql-queries/get-room db {:room-name old-room-name}))]
      (sql-queries/update-room! db {:uuid uuid :room-name new-room-name})))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [db]}]
    (sql-queries/occupied-rooms db))
  (all-active-users [{:keys [db]}]
    (sql-queries/all-active-users db))
  (users-in-room [{:keys [db]} room-name]
    (sql-queries/users-in-room db room-name))
  (current-room-name [{:keys [db]} username]
    (sql-queries/current-room-name db username))
  (room [{:keys [db]} room-name]
    (sql-queries/get-room db {:name room-name}))
  (chat-history [{:keys [db]} room-name]
    (sql-queries/get-messages-for-room db room-name))
  planex-api/IOutbox
  (outbox-write! [{:keys [db]} evt]
    (sql-queries/insert-outbox-event! db evt))
  (outbox-read [{:keys [db]}]
    (sql-queries/get-outbox-events db))
  (outbox-get [{:keys [db]} event]
    (sql-queries/get-outbox-event db event))
  (outbox-delete! [{:keys [db]} event]
    (sql-queries/delete-outbox-event! db event)))
