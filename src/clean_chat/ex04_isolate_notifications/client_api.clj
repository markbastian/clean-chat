(ns clean-chat.ex04-isolate-notifications.client-api
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defn get-client [clients client-id]
  (get-in clients [:client-id client-id]))

(defn clients-by-transform [clients]
  (get clients :transforms))

(defn all-clients [clients]
  (vals (get clients :client-id)))

(defn add-client! [state {:keys [client-id transform] :as client}]
  (if-not (get-client @state client-id)
    (do
      (log/debugf "Adding client: %s" client-id)
      (swap! state (fn [clients]
                     (-> clients
                         (assoc-in [:client-id client-id] client)
                         (assoc-in [:transforms transform :client-id client-id] client)))))
    (log/debugf "Client '%s' already exists. Not adding." client-id)))

(defn remove-client! [state client-id]
  (when (get-client @state client-id)
    (log/debugf "Removing client: %s" client-id)
    (swap! state (fn [clients]
                   (let [{:keys [transform]} (get-client clients client-id)]
                     (-> clients
                         (update :client-id dissoc client-id)
                         (update-in [:transforms transform :client-id] dissoc client-id)))))))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [client-id ws]} message]
  (log/tracef "Sending %s to %s via ws." message client-id)
  (jetty/send! ws message))

(defn broadcast! [db client-ids message]
  (doseq [client-id client-ids :let [client (get-client db client-id)] :when client]
    (send! client message)))