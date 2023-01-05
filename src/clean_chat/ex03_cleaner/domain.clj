(ns clean-chat.ex03-cleaner.domain
  (:require
    [clean-chat.ex03-cleaner.client-api :as client-api]
    [clean-chat.ex03-cleaner.queries :as queries]
    [datascript.core :as d]
    [clean-chat.ex03-cleaner.htmx-notifications :as htmx-notifications]))

(defn create-chat-message! [{:keys [clients conn]} username message]
  ;;Business logic
  (let [room-name (queries/current-room-name @conn username)]
    (d/transact! conn [{:message                message
                        :nanos-since-unix-epoch (System/nanoTime)
                        :user                   {:username username}
                        :room                   {:room-name room-name}}])
    ;; Notifications
    (let [client (client-api/get-client clients username)]
      (htmx-notifications/broadcast-to-room
        clients @conn room-name (format "%s: %s" username message))
      (htmx-notifications/notify-update-chat-prompt client room-name))))

(defn join-room! [{:keys [clients conn]} {:keys [username room-name]}]
  ;; Business logic
  (let [old-room-name (queries/current-room-name @conn username)
        room-will-be-created? (not (queries/room-exists? @conn room-name))]
    (when-not (= room-name old-room-name)
      (let [tx-data [{:username username
                      :room     {:room-name room-name}}]
            {:keys [db-after]} (d/transact! conn tx-data)]
        ;; Notifications
        (if old-room-name
          (htmx-notifications/broadcast-leave-room clients db-after username old-room-name)
          (htmx-notifications/broadcast-update-active-user-list clients db-after))
        (when (or room-will-be-created? (nil? old-room-name))
          (htmx-notifications/broadcast-update-room-list clients db-after))
        (htmx-notifications/broadcast-enter-room clients db-after username room-name)))))

(defn leave-chat! [{:keys [clients conn]} username]
  (when-some [old-room-name (queries/current-room-name @conn username)]
    ;; Business logic
    (let [tx-data [[:db.fn/retractAttribute [:username username] :room]]
          {:keys [db-after]} (d/transact! conn tx-data)]
      ;; Notifications
      (htmx-notifications/broadcast-leave-room clients db-after username old-room-name)
      (htmx-notifications/broadcast-update-active-user-list clients db-after))))

(defn rename-room! [{:keys [clients conn]} {:keys [username room-name]}]
  (when-not (queries/room-exists? @conn room-name)
    (let [old-room-name (queries/current-room-name @conn username)
          id (:db/id (queries/room @conn old-room-name))]
      (when (and id (not= room-name old-room-name))
        ;; Business logic
        (let [tx-data [[:db/add id :room-name room-name]]
              {:keys [db-after]} (d/transact! conn tx-data)]
          ;; Notifications
          (htmx-notifications/broadcast-update-room-list clients db-after)
          (htmx-notifications/broadcast-to-room
            clients
            @conn
            room-name
            (format "Room name changed to %s" room-name))
          (doseq [username (queries/all-active-users @conn)
                  :let [client (client-api/get-client clients username)]]
            (htmx-notifications/notify-foo client room-name)))))))

;; Refactorings
;; - Separate clients from state
