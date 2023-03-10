(ns clean-chat.stage06-sql.client-notifications-htmx
  (:require [clean-chat.stage06-sql.api.client-notifications :as client-notifications]
            [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.client-api :as client-api]
            [clean-chat.stage06-sql.htmx-notifications :as htmx-notifications]
            [clojure.tools.logging :as log]))

(defmethod client-notifications/notify-client [:htmx :join-chat]
  [{:keys [clients] :as context} {:keys [username]}]
  (log/debugf "%s is joining" username)
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/notify-update-room-list context client))
  (htmx-notifications/broadcast-update-active-user-list context clients))

(defmethod client-notifications/notify-client [:htmx :leave-chat]
  [{:keys [clients] :as context} {:keys [username]}]
  (log/debugf "%s is leaving" username)
  (htmx-notifications/broadcast-update-active-user-list context clients))

(defmethod client-notifications/notify-client [:htmx :create-message]
  [{:keys [clients] :as context} {:keys [username room-name message]}]
  (let [client (client-api/get-client clients username)]
    (htmx-notifications/broadcast-to-room
     context room-name (format "%s: %s" username message))
    (htmx-notifications/notify-update-chat-prompt client room-name)))

(defmethod client-notifications/notify-client [:htmx :enter-room]
  [context {:keys [username room-name]}]
  (log/debugf "%s is entering %s" username room-name)
  (htmx-notifications/broadcast-enter-room context username room-name))

(defmethod client-notifications/notify-client [:htmx :leave-room]
  [context {:keys [username room-name]}]
  (log/debugf "%s is leaving %s" username room-name)
  (htmx-notifications/broadcast-leave-room context username room-name))

(defmethod client-notifications/notify-client [:htmx :create-room]
  [{:keys [clients] :as context} {:keys [room-name]}]
  (log/debugf "%s created." room-name)
  (htmx-notifications/broadcast-update-room-list context clients))

(defmethod client-notifications/notify-client [:htmx :rename-room]
  [{:keys [clients] :as context} {:keys [old-room-name new-room-name]}]
  (log/debugf "Renaming %s to %s" old-room-name new-room-name)
  (htmx-notifications/broadcast-update-room-list context clients)
  (htmx-notifications/broadcast-to-room context new-room-name
                                        (format "Room name changed to %s" new-room-name))
  (doseq [username (chat-api/users-in-room context new-room-name)
          :let [client (client-api/get-client clients username)]]
    (htmx-notifications/notify-update-room-names client new-room-name)))
