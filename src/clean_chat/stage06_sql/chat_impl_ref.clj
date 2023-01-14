(ns clean-chat.stage06-sql.chat-impl-ref
  (:require [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.queries-datascript :as queries]
            [clean-chat.stage06-sql.planex-api :as planex-api]))

(defrecord RefChat [db outbox])

(defn update-db [this fn & args]
  (apply update this :db commute fn args))

(defn update-outbox [this tx-fn & args]
  (apply update this :outbox commute tx-fn args))

(extend-type RefChat
  chat-api/IChatEvents
  (create-message! [this m]
    (update-db this queries/create-message m))
  (join-chat! [this m]
    (update-db this queries/join-chat m))
  (leave-chat! [this m]
    (update-db this queries/leave-room m))
  (create-room! [this {:keys [room-name]}]
    (update-db this queries/insert-room room-name))
  (enter-room! [this {:keys [_username _room-name] :as m}]
    (update-db this queries/enter-room m))
  (leave-room! [this {:keys [_username] :as m}]
    (update-db this queries/leave-room m))
  (rename-room! [this {:keys [_old-room-name _new-room-name] :as m}]
    (update-db this queries/rename-room m))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [db]}]
    (queries/occupied-rooms @db))
  (all-active-users [{:keys [db]}]
    (queries/all-active-users @db))
  (users-in-room [{:keys [db]} room-name]
    (queries/users-in-room @db room-name))
  (current-room-name [{:keys [db]} username]
    (queries/current-room-name @db username))
  (room [{:keys [db]} room-name]
    (queries/room @db room-name))
  (chat-history [{:keys [db]} room-name]
    (queries/chat-history @db room-name))
  planex-api/IOutbox
  (outbox-write! [this evt]
    (update-outbox this conj evt))
  (outbox-read [{:keys [outbox]}] @outbox)
  (outbox-get [{:keys [outbox]} {to-find :uuid}]
    (first (filter (fn [{:keys [uuid]}] (= to-find uuid)) @outbox)))
  (outbox-delete! [this {to-be-removed :uuid}]
    (update
      this :outbox commute
      (fn [events] (vec (remove (fn [{:keys [uuid]}] (= uuid to-be-removed)) events))))
    (update-outbox this identity)))