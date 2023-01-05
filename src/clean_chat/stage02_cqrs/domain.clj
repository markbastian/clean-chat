(ns clean-chat.stage02-cqrs.domain
  (:require
    [clean-chat.stage02-cqrs.queries :as queries]
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [clean-chat.stage02-cqrs.htmx-notifications :as htmx-notifications]))

(defn create-chat-message! [{:keys [conn]} username message]
  (let [room-name (queries/current-room-name @conn username)]
    (d/transact! conn [{:message                message
                        :nanos-since-unix-epoch (System/nanoTime)
                        :user                   {:username username}
                        :room                   {:room-name room-name}}])
    (log/infof
      "Broadcasting message '%s' from '%s' to '%s'." message username room-name)
    (htmx-notifications/broadcast-to-room
      @conn room-name (format "%s: %s" username message)))
  (htmx-notifications/notify-update-chat-prompt @conn username))

(defn join-room! [{:keys [conn]} {:keys [username room-name] :as entity}]
  (let [old-room-name (queries/current-room-name @conn username)
        room-will-be-created? (not (queries/room-exists? @conn room-name))]
    (when-not (= room-name old-room-name)
      (let [tx-data [(-> entity
                         (dissoc :room-name :command)
                         (assoc :room {:room-name room-name}))]
            {:keys [db-after]} (d/transact! conn tx-data)]
        (if old-room-name
          (htmx-notifications/broadcast-leave-room db-after username old-room-name)
          (htmx-notifications/broadcast-update-active-user-list db-after))
        (when (or room-will-be-created? (nil? old-room-name))
          (htmx-notifications/broadcast-update-room-list db-after))
        (htmx-notifications/broadcast-enter-room db-after username room-name)))))

(defn leave-chat! [{:keys [conn]} username]
  (when-some [old-room-name (queries/current-room-name @conn username)]
    (let [tx-data [[:db.fn/retractAttribute [:username username] :ws]
                   [:db.fn/retractAttribute [:username username] :room]]
          {:keys [db-after]} (d/transact! conn tx-data)]
      (htmx-notifications/broadcast-leave-room db-after username old-room-name)
      (htmx-notifications/broadcast-update-active-user-list db-after))))

(defn rename-room! [{:keys [conn]} {:keys [username room-name]}]
  (when-not (queries/room-exists? @conn room-name)
    (let [old-room-name (queries/current-room-name @conn username)
          id (:db/id (queries/room @conn old-room-name))]
      (when (and id (not= room-name old-room-name))
        (let [tx-data [[:db/add id :room-name room-name]]
              {:keys [db-after]} (d/transact! conn tx-data)]
          (htmx-notifications/broadcast-update-room-list db-after)
          (htmx-notifications/broadcast-to-room
            @conn
            room-name
            (format "Room name changed to %s" room-name))
          (doseq [username (queries/all-active-users @conn)]
            (htmx-notifications/notify-update-chat-prompt @conn username)
            (htmx-notifications/notify-update-room-change-link @conn username)))))))

;; Refactorings
;; - Extract commands
;; - Move notifications to htmx-notifications
;; - Move queries to queries
;;
;; Note the cost! This isn't free!
