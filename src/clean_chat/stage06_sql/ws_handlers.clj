(ns clean-chat.stage06-sql.ws-handlers
  (:require [clean-chat.pages :as chat-pages]
            [clean-chat.stage06-sql.broker-ref :as broker-ref]
    ;[clean-chat.stage06-sql.broker-sql :as broker-sql]
            [clean-chat.stage06-sql.client-manager :as client-api]
            [clean-chat.utils :as u]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5]]
            [ring.adapter.jetty9 :as jetty]))

(defn notify-and-close-login-failure [title ws]
  (jetty/send! ws (html5 (chat-pages/show-chat-login title {:hx-swap-oob "true"})))
  (jetty/close! ws))

(defn on-connect [{:keys [title path-params client-manager] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if-not (client-api/get-client @client-manager username)
      (do
        (client-api/add-client! client-manager {:client-id username
                                                :transport :ws
                                                :ws        ws
                                                :transform :htmx})
        (broker-ref/process-command
         (-> context
             (dissoc :client-manager)
             (assoc :clients @client-manager))
         {:command   :join-chat
          :username  username
          :room-name room-name}))
      (notify-and-close-login-failure title ws))))

(defn on-text [{:keys [path-params client-manager] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        {:keys [HEADERS] :as json} (u/read-json text-message)
        command (-> json
                    (assoc
                     :username username
                     :command (some-> HEADERS :HX-Trigger-Name keyword))
                    (dissoc :HEADERS))]
    (broker-ref/process-command
     (-> context
         (dissoc :client-manager)
         (assoc :clients @client-manager))
     command)))

(defn on-close [{:keys [path-params client-manager] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params]
    (log/debugf "on-close triggered for user: %s" username)
    (client-api/remove-client! client-manager username)
    (let [command {:command :leave-chat :username username}]
      (broker-ref/process-command
       (-> context
           (dissoc :client-manager)
           (assoc :clients @client-manager))
       command))))

(defn on-error [{:keys [path-params client-manager] :as context} ws err]
  (let [{:keys [username]} path-params]
    (log/debugf "on-error triggered for user: %s" username)
    (client-api/remove-client! client-manager username)
    (let [command {:command :leave-chat :username username}]
      (broker-ref/process-command
       (-> context
           (dissoc :client-manager)
           (assoc :clients @client-manager))
       command))
    (println ws)
    (println err)
    (println "ERROR")))
