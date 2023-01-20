(ns clean-chat.stage06-sql.htmx-notifications
  (:require [clean-chat.pages :as chat-pages]
            [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.client-api :as client-api]
            [hiccup.page :refer [html5]]))

(defn notify-update-chat-prompt [client room-name]
  (let [html (html5 (chat-pages/chat-prompt
                     room-name {:autofocus "true" :hx-swap-oob "true"}))]
    (client-api/send! client html)))

(defn notify-update-room-change-link [client room-name]
  (let [html (html5 (chat-pages/room-change-link
                     room-name {:hx-swap-oob "true"}))]
    (client-api/send! client html)))

(defn notify-update-room-names [client room-name]
  (notify-update-chat-prompt client room-name)
  (notify-update-room-change-link client room-name))

(defn notify-update-room-list [context client]

  (let [data (chat-api/occupied-rooms context)
        html (html5 (chat-pages/sidebar-room-names data))]
    (client-api/send! client html)))

(defn broadcast-update-room-list [context clients]
  (let [data (chat-api/occupied-rooms context)
        html (html5 (chat-pages/sidebar-room-names data))
        usernames (chat-api/all-active-users context)]
    (client-api/broadcast! clients usernames html)))

(defn broadcast-update-active-user-list [context clients]
  (let [data (chat-api/all-active-users context)
        html (html5 (chat-pages/sidebar-usernames data))]
    (client-api/broadcast! clients data html)))

(defn broadcast-to-room [{:keys [clients] :as context} room-name message]
  (let [html (html5
              (chat-pages/notifications-pane
               {:hx-swap-oob "beforeend"}
               [:div [:i message]]))
        usernames (chat-api/users-in-room context room-name)]
    (client-api/broadcast! clients usernames html)))

(defn notify-reset-room [{:keys [clients] :as context} username]
  (let [room-name (chat-api/current-room-name context username)
        chat-history (chat-api/chat-history context room-name)
        divs (mapv
              (fn [{:keys [username message]}]
                [:div [:i (format "%s: %s" username message)]])
              chat-history)
        html (html5 (apply
                     chat-pages/notifications-pane
                     {:hx-swap-oob "true"}
                     divs))]
    (client-api/send! (client-api/get-client clients username) html)))

(defn broadcast-enter-room [{:keys [clients] :as context} username new-room-name]
  (let [client (client-api/get-client clients username)]
    (notify-reset-room context username)
    (let [message (format "%s joined %s" username new-room-name)]
      (broadcast-to-room context new-room-name message))
    (notify-update-room-names client new-room-name)))

(defn broadcast-leave-room [context username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room context old-room-name message)))
