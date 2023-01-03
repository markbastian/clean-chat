(ns clean-chat.ex01-unclean.domain
  (:require
    [clean-chat.pages :as chat-pages]
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [clean-chat.utils :as u]
    [hiccup.page :refer [html5]]
    [ring.adapter.jetty9 :as jetty]))

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-users-query
  '[:find [?username ...]
    :in $
    :where
    [_ :username ?username]])

(defn occupied-rooms [db]
  (->> (d/q all-rooms-query db)
       sort
       (map (fn [room-name]
              (chat-pages/sidebar-sublist-item
                room-name
                {:ws-send "true"
                 :name    "change-room"
                 :method  :post
                 :hx-vals (u/to-json-str {:room-name room-name})})))))

(defn all-users [db]
  (->> (d/q all-users-query db)
       sort
       (map chat-pages/sidebar-sublist-item)))

(def all-ws-query
  '[:find [?ws ...] :in $ :where [?e :ws ?ws]])

(def room-name->ws-query
  '[:find [?ws ...]
    :in $ ?room-name
    :where
    [?e :ws ?ws]
    [?e :room-name ?room-name]])

(defn current-room-name [db username]
  (:room-name (d/entity db [:username username])))

(defn update-chat-prompt [db username]
  (let [{:keys [ws room-name]} (d/entity db [:username username])
        html (chat-pages/chat-prompt room-name {:autofocus   "true"
                                                :hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn update-room-change-link [db username]
  (let [{:keys [ws room-name]} (d/entity db [:username username])
        html (chat-pages/room-change-link room-name {:hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn broadcast-update-room-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "roomList"} (occupied-rooms db))
        room-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-update-user-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-users db))
        room-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-to-room [db room-name message]
  (let [html (chat-pages/notifications-pane
               {:hx-swap-oob "beforeend"}
               [:div [:i message]])]
    (doseq [client (d/q room-name->ws-query db room-name)]
      (jetty/send! client (html5 html)))))

(defn broadcast-enter-room [db username new-room-name]
  (let [message (format "%s joined %s" username new-room-name)]
    (broadcast-to-room db new-room-name message))
  (update-chat-prompt db username)
  (update-room-change-link db username))

(defn broadcast-leave-room [db username old-room-name]
  (let [message (format "%s left %s" username old-room-name)]
    (broadcast-to-room db old-room-name message)))

(defn broadcast-chat-message [db username message]
  (let [message (format "%s: %s" username message)
        room-name (:room-name (d/entity db [:username username]))]
    (log/infof "Broadcasting message '%s' from '%s' to '%s'." message username room-name)
    (broadcast-to-room db room-name message))
  (update-chat-prompt db username))

(defn join-room! [{:keys [conn]} {:keys [username room-name] :as entity}]
  (let [old-room-name (current-room-name @conn username)]
    (when-not (= room-name old-room-name)
      (let [{:keys [db-after]} (d/transact! conn [entity])]
        (broadcast-leave-room db-after username old-room-name)
        (broadcast-enter-room db-after username room-name)
        (broadcast-update-room-list db-after)
        (broadcast-update-user-list db-after)))))

(defn leave-chat! [{:keys [conn]} username]
  (let [tx-data [[:db/retractEntity [:username username]]]
        {:keys [db-before db-after]} (d/transact! conn tx-data)
        old-room-name (current-room-name db-before username)]
    (broadcast-leave-room db-after username old-room-name)
    (broadcast-update-room-list db-after)
    (broadcast-update-user-list db-after)))
