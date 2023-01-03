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

(def all-active-users-query
  '[:find ?username ?room-name
    :keys username room-name
    :in $
    :where
    [?e :username ?username]
    [?e :room ?r]
    [?r :room-name ?room-name]])

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

(defn all-active-users [db]
  (->> (d/q all-active-users-query db)
       (map :username)
       sort
       (map chat-pages/sidebar-sublist-item)))

(def all-ws-query
  '[:find [?ws ...] :in $ :where [?e :ws ?ws]])

(def username->ws+room-query
  '[:find ?ws ?room-name
    :keys ws room-name
    :in $ ?username
    :where
    [?e :username ?username]
    [?e :ws ?ws]
    [?e :room ?r]
    [?r :room-name ?room-name]])

(defn ws+room-name [db username]
  (first
    (d/q
      username->ws+room-query
      db username)))

(defn current-room-name [db username]
  (some-> db (d/entity [:username username]) :room :room-name))

(defn notify-and-close-login-failure [ws]
  (jetty/send! ws (html5 (chat-pages/show-chat-login {:hx-swap-oob "true"})))
  (jetty/close! ws))

(defn notify-update-chat-prompt [db username]
  (let [{:keys [ws room-name]} (ws+room-name db username)
        html (chat-pages/chat-prompt room-name {:autofocus   "true"
                                                :hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn notify-update-room-change-link [db username]
  (let [{:keys [ws room-name]} (ws+room-name db username)
        html (chat-pages/room-change-link room-name {:hx-swap-oob "true"})]
    (jetty/send! ws (html5 html))))

(defn broadcast-update-room-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "roomList"} (occupied-rooms db))
        room-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client room-list-html))))

(defn broadcast-update-active-user-list [db]
  (let [html (chat-pages/sidebar-sublist {:id "userList"} (all-active-users db))
        user-list-html (html5 html)]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client user-list-html))))

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

;; Note that in this situation messages are ephemeral -- we are only tracking
;; the room and user states.
(defn broadcast-chat-message [db username message]
  (let [message (format "%s: %s" username message)
        room-name (current-room-name db username)]
    (log/infof "Broadcasting message '%s' from '%s' to '%s'." message username room-name)
    (broadcast-to-room db room-name message))
  (notify-update-chat-prompt db username))

(defn join-room! [{:keys [conn]} {:keys [username room-name] :as entity}]
  (let [old-room-name (current-room-name @conn username)
        room-will-be-created? (nil? (d/entity @conn [:room-name room-name]))]
    (when-not (= room-name old-room-name)
      (let [tx-data [(-> entity
                         (dissoc :room-name)
                         (assoc :room {:room-name room-name}))]
            ;; Write/mutate operation
            {:keys [db-after]} (d/transact! conn tx-data)]
        ;; These statements contain two concerns: effects & notifications
        (when old-room-name
          (broadcast-leave-room db-after username old-room-name))
        (broadcast-enter-room db-after username room-name)
        (when room-will-be-created?
          (broadcast-update-room-list db-after))
        (broadcast-update-active-user-list db-after)))))

(defn leave-chat! [{:keys [conn]} username]
  (let [tx-data [[:db.fn/retractAttribute [:username username] :ws]
                 [:db.fn/retractAttribute [:username username] :room]]
        ;; Write/mutate operation
        {:keys [db-before db-after]} (d/transact! conn tx-data)
        old-room-name (current-room-name db-before username)]
    ;; These statements contain two concerns: effects & notifications
    (broadcast-leave-room db-after username old-room-name)
    (broadcast-update-active-user-list db-after)))
