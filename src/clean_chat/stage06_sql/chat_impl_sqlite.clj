(ns clean-chat.stage06-sql.chat-impl-sqlite
  (:require [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.sql-queries :as sql-queries]))

(defrecord SqlChat [db])

(extend-type SqlChat
  chat-api/IChatEvents
  (create-message [{:keys [db]} event]
    (sql-queries/insert-message! db event))
  (join-chat [{:keys [db]} event]
    (sql-queries/upsert-user! db event))
  (leave-chat [{:keys [db]} event]
    (sql-queries/upsert-user! db (assoc event :room-name nil)))
  (create-room [{:keys [db]} event]
    (sql-queries/insert-room! db event))
  (enter-room [{:keys [db]} event]
    (sql-queries/upsert-user! db event))
  (leave-room [{:keys [db]} event]
    (sql-queries/upsert-user! db (assoc event :room-name nil)))
  (rename-room [{:keys [db]} {:keys [old-room-name new-room-name]}]
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
    (sql-queries/get-messages-for-room db room-name)))