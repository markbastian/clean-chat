(ns clean-chat.stage03-isolate-clients.htmx-notifications
  (:require [clean-chat.pages :as chat-pages]
            [clean-chat.stage03-isolate-clients.client-api :as client-api]
            [clean-chat.stage03-isolate-clients.queries :as queries]
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

(defn broadcast-update-room-list [clients db]
  (let [data (queries/occupied-rooms db)
        html (html5 (chat-pages/sidebar-room-names data))
        usernames (queries/all-active-users db)]
    (client-api/broadcast! clients usernames html)))

(defn broadcast-update-active-user-list [clients db]
  (let [data (queries/all-active-users db)
        html (html5 (chat-pages/sidebar-usernames data))]
    (client-api/broadcast! clients data html)))

(defn broadcast-to-room [clients db room-name message]
  (let [html (html5
               (chat-pages/notifications-pane
                 {:hx-swap-oob "beforeend"}
                 [:div [:i message]]))
        usernames (queries/users-in-room db room-name)]
    (client-api/broadcast! clients usernames html)))

(defn notify-reset-room [clients db username]
  (let [room-name (queries/current-room-name db username)
        chat-history (queries/chat-history db room-name)
        divs (mapv
               (fn [{:keys [username message]}]
                 [:div [:i (format "%s: %s" username message)]])
               chat-history)
        html (html5 (apply
                      chat-pages/notifications-pane
                      {:hx-swap-oob "true"}
                      divs))]
    (client-api/send! (client-api/get-client clients username) html)))

(defn broadcast-enter-room [clients db username new-room-name]
  (let [client (client-api/get-client clients username)]
    (notify-reset-room clients db username)
    (let [message (format "%s joined %s" username new-room-name)]
      (broadcast-to-room clients db new-room-name message))
    (notify-update-room-names client new-room-name)))

(defn broadcast-leave-room [clients db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room clients db old-room-name message)))