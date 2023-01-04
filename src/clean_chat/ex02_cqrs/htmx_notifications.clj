(ns clean-chat.ex02-cqrs.htmx-notifications
  (:require [clean-chat.pages :as chat-pages]
            [clean-chat.ex02-cqrs.queries :as queries]
            [datascript.core :as d]
            [ring.adapter.jetty9 :as jetty]
            [hiccup.page :refer [html5]]))

(defn notify-and-close-login-failure [title ws]
  (jetty/send! ws (html5 (chat-pages/show-chat-login title {:hx-swap-oob "true"})))
  (jetty/close! ws))

(defn notify-update-chat-prompt [db username]
  (let [{:keys [ws room-name]} (queries/ws+room-name db username)
        html (chat-pages/chat-prompt room-name {:autofocus   "true"
                                                :hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn notify-update-room-change-link [db username]
  (let [{:keys [ws room-name]} (queries/ws+room-name db username)
        html (chat-pages/room-change-link room-name {:hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn broadcast-update-room-list [db]
  (let [data (queries/occupied-rooms db)
        html (html5 (chat-pages/sidebar-room-names data))]
    (doseq [client (d/q queries/all-ws-query db)]
      (jetty/send! client html))))

(defn broadcast-update-active-user-list [db]
  (let [data (queries/all-active-users db)
        html (html5 (chat-pages/sidebar-usernames data))]
    (doseq [client (d/q queries/all-ws-query db)]
      (jetty/send! client html))))

(defn broadcast-to-room [db room-name message]
  (let [html (chat-pages/notifications-pane
               {:hx-swap-oob "beforeend"}
               [:div [:i message]])]
    (doseq [client (->> (d/entity db [:room-name room-name])
                        :_room
                        (map :ws)
                        (filter identity))]
      (jetty/send! client (html5 html)))))

(defn broadcast-enter-room [db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room db new-room-name message))
  (notify-update-chat-prompt db username)
  (notify-update-room-change-link db username))

(defn broadcast-leave-room [db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room db old-room-name message)))