(ns clean-chat.stage05-planbox.htmx-events
  (:require
    [clean-chat.stage05-planbox.client-api :as client-api]
    [clean-chat.stage05-planbox.planex-api :as planex-api]
    [clean-chat.stage05-planbox.htmx-notifications :as htmx-notifications]
    [clean-chat.stage05-planbox.queries :as queries]
    [clojure.tools.logging :as log]))

(defmethod planex-api/dispatch-event [:htmx :join-chat]
  [{:keys [clients db]} {:keys [username]}]
  (log/debugf "%s is joining" username)
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/notify-update-room-list client db))
  (htmx-notifications/broadcast-update-active-user-list clients db))

(defmethod planex-api/dispatch-event [:htmx :leave-chat]
  [{:keys [clients db]} {:keys [username]}]
  (log/debugf "%s is leaving" username)
  (htmx-notifications/broadcast-update-active-user-list clients db))

(defmethod planex-api/dispatch-event [:htmx :create-message]
  [{:keys [clients db]} {:keys [username room-name message]}]
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/broadcast-to-room
      clients db room-name (format "%s: %s" username message))
    (htmx-notifications/notify-update-chat-prompt client room-name)))

(defmethod planex-api/dispatch-event [:htmx :enter-room]
  [{:keys [clients db]} {:keys [username room-name]}]
  (log/debugf "%s is entering %s" username room-name)
  (htmx-notifications/broadcast-enter-room clients db username room-name))
(defmethod planex-api/dispatch-event [:htmx :leave-room]
  [{:keys [clients db]} {:keys [username room-name]}]
  (log/debugf "%s is leaving %s" username room-name)
  (htmx-notifications/broadcast-leave-room clients db username room-name))

(defmethod planex-api/dispatch-event [:htmx :create-room]
  [{:keys [clients db]} {:keys [room-name]}]
  (log/debugf "%s created." room-name)
  (htmx-notifications/broadcast-update-room-list clients db))

(defmethod planex-api/dispatch-event [:htmx :rename-room]
  [{:keys [clients db]} {:keys [old-room-name new-room-name]}]
  (log/debugf "Renaming %s to %s" old-room-name new-room-name)
  (htmx-notifications/broadcast-update-room-list clients db)
  (htmx-notifications/broadcast-to-room
    clients db new-room-name
    (format "Room name changed to %s" new-room-name))
  (doseq [username (queries/all-active-users db)
          :let [client (client-api/get-client clients username)]]
    (htmx-notifications/notify-update-room-names client new-room-name)))
