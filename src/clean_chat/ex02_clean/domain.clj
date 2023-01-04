(ns clean-chat.ex02-clean.domain
  (:require
    [clean-chat.ex02-clean.queries :as queries]
    [clean-chat.ex02-clean.htmx-notifications :as htmx-notifications]
    [clojure.tools.logging :as log]
    [datascript.core :as d]))

(defn create-chat-message! [{:keys [conn]} username message]
  (let [message (format "%s: %s" username message)
        room-name (queries/current-room-name @conn username)]
    (log/infof "Broadcasting message '%s' from '%s' to '%s'." message username room-name)
    (htmx-notifications/broadcast-to-room @conn room-name message))
  (htmx-notifications/notify-update-chat-prompt @conn username))

(defn join-room! [{:keys [conn]} {:keys [username room-name] :as entity}]
  (let [old-room-name (queries/current-room-name @conn username)
        room-will-be-created? (nil? (d/entity @conn [:room-name room-name]))]
    (when-not (= room-name old-room-name)
      (let [tx-data [(-> entity
                         (dissoc :room-name)
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
