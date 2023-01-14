(ns clean-chat.stage01-unclean.domain
  (:require
   [clean-chat.pages :as chat-pages]
   [clojure.tools.logging :as log]
   [datascript.core :as d]
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
  (sort (d/q all-rooms-query db)))

(defn all-active-users [db]
  (->> (d/q all-active-users-query db) (map :username) sort))

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

(defn notify-and-close-login-failure [title ws]
  (jetty/send! ws (html5 (chat-pages/show-chat-login title {:hx-swap-oob "true"})))
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
  (let [data (occupied-rooms db)
        html (html5 (chat-pages/sidebar-room-names data))]
    (doseq [client (d/q all-ws-query db)]
      (jetty/send! client html))))

(defn broadcast-update-active-user-list [db]
  (let [data (all-active-users db)
        html (html5 (chat-pages/sidebar-usernames data))]
    (doseq [client (d/q all-ws-query db)]
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

;; Note that in this situation messages are ephemeral -- we are only tracking
;; the room and user states.
(defn create-chat-message! [{:keys [conn]} username message]
  (let [message (format "%s: %s" username message)
        room-name (current-room-name @conn username)]
    (log/infof "Broadcasting message '%s' from '%s' to '%s'." message username room-name)
    (broadcast-to-room @conn room-name message))
  (notify-update-chat-prompt @conn username))

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
        (if old-room-name
          (broadcast-leave-room db-after username old-room-name)
          (broadcast-update-active-user-list db-after))
        (when (or room-will-be-created? (nil? old-room-name))
          (broadcast-update-room-list db-after))
        (broadcast-enter-room db-after username room-name)))))

(defn leave-chat! [{:keys [conn]} username]
  (when (:room (d/entity @conn [:username username]))
    (let [tx-data [[:db.fn/retractAttribute [:username username] :ws]
                   [:db.fn/retractAttribute [:username username] :room]]
          ;; Write/mutate operation
          {:keys [db-before db-after]} (d/transact! conn tx-data)
          old-room-name (current-room-name db-before username)]
      ;; These statements contain two concerns: effects & notifications
      (broadcast-leave-room db-after username old-room-name)
      (broadcast-update-active-user-list db-after))))

;; Challenges:
;; - Notifications are coupled to effects -- We can't independently handle effects if desired
;; - Clients are baked into the context and are hardwired for a single type (ws)
;; - Notifications are all tied to the final presentation (htmx)
;; - No tests -- How would you test this?
;; Pros:
;; - This is actually really easy to read and follow
;;
;; Should I even refactor this?
;; What's the business or personal value?
;; What do I want to do?
;; - Add new client types?
;; - Add new commands?
;; - Add new response types?
;; - Persist my data?
;; - Add some sort of testing?
;;
;; If I don't know what I want to do then I should build something else.
