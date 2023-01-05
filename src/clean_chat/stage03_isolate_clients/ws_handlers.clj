(ns clean-chat.stage03-isolate-clients.ws-handlers
  (:require [clean-chat.stage03-isolate-clients.client-api :as client-api]
            [clean-chat.stage03-isolate-clients.commands :as commands]
            [clean-chat.pages :as chat-pages]
            [clean-chat.utils :as u]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5]]
            [ring.adapter.jetty9 :as jetty]))

(defn notify-and-close-login-failure [title ws]
  (jetty/send! ws (html5 (chat-pages/show-chat-login title {:hx-swap-oob "true"})))
  (jetty/close! ws))

(defn on-connect [{:keys [title path-params clients] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if-not (client-api/get-client @clients username)
      (do
        (client-api/add-client! clients {:client-id username
                                         :transport :ws
                                         :ws        ws})
        (commands/dispatch-command context {:command   :change-room
                                            :username  username
                                            :room-name room-name}))
      (notify-and-close-login-failure title ws))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        {:keys [HEADERS] :as json} (u/read-json text-message)
        command (-> json
                    (assoc
                      :username username
                      :command (some-> HEADERS :HX-Trigger-Name keyword))
                    (dissoc :HEADERS))]
    (commands/dispatch-command context command)))

(defn on-close [{:keys [path-params clients] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params]
    (log/debugf "on-close triggered for user: %s" username)
    (client-api/remove-client! clients username)
    (let [command {:command :leave-chat :username username}]
      (commands/dispatch-command context command))))

(defn on-error [{:keys [path-params clients] :as context} ws err]
  (let [{:keys [username]} path-params]
    (log/debugf "on-error triggered for user: %s" username)
    (client-api/remove-client! clients username)
    (let [command {:command :leave-chat :username username}]
      (commands/dispatch-command context command))
    (println ws)
    (println err)
    (println "ERROR")))