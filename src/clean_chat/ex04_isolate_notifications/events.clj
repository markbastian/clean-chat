(ns clean-chat.ex04-isolate-notifications.events
  (:require
    [clean-chat.ex04-isolate-notifications.client-api :as client-api]
    [clean-chat.ex04-isolate-notifications.htmx-notifications :as htmx-notifications]
    [clean-chat.ex04-isolate-notifications.queries :as queries]
    [clojure.tools.logging :as log]))

(defmulti dispatch-event (fn [{:keys [transform]} {:keys [event]}]
                           [transform event]))

(defmethod dispatch-event :default [{:keys [transform]}
                                    {:keys [event]}]
  (log/warnf "Unhandled dispatch value: [%s %s]" transform event))

(defmethod dispatch-event [:htmx :join-chat]
  [{:keys [clients db]} {:keys [username]}]
  (log/debugf "%s is joining" username)
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/notify-update-room-list client db))
  (htmx-notifications/broadcast-update-active-user-list clients db))

(defmethod dispatch-event [:htmx :leave-chat]
  [{:keys [clients db]} {:keys [username]}]
  (log/debugf "%s is leaving" username)
  (htmx-notifications/broadcast-update-active-user-list clients db))

(defmethod dispatch-event [:htmx :create-message]
  [{:keys [clients db]} {:keys [username room-name message]}]
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/broadcast-to-room
      clients db room-name (format "%s: %s" username message))
    (htmx-notifications/notify-update-chat-prompt client room-name)))

(defmethod dispatch-event [:htmx :enter-room]
  [{:keys [clients db]} {:keys [username room-name]}]
  (log/debugf "%s is entering %s" username room-name)
  (htmx-notifications/broadcast-enter-room clients db username room-name))
(defmethod dispatch-event [:htmx :leave-room]
  [{:keys [clients db]} {:keys [username room-name]}]
  (log/debugf "%s is leaving %s" username room-name)
  (htmx-notifications/broadcast-leave-room clients db username room-name))

(defmethod dispatch-event [:htmx :create-room]
  [{:keys [clients db]} {:keys [room-name]}]
  (log/debugf "%s created." room-name)
  (htmx-notifications/broadcast-update-room-list clients db))

(defmethod dispatch-event [:htmx :rename-room]
  [{:keys [clients db]} {:keys [old-room-name new-room-name]}]
  (log/debugf "Renaming %s to %s" old-room-name new-room-name)
  (htmx-notifications/broadcast-update-room-list clients db)
  (htmx-notifications/broadcast-to-room
    clients db new-room-name
    (format "Room name changed to %s" new-room-name))
  (doseq [username (queries/all-active-users db)
          :let [client (client-api/get-client clients username)]]
    (htmx-notifications/notify-update-room-names client new-room-name)))
