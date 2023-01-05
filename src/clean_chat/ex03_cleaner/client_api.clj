(ns clean-chat.ex03-cleaner.client-api
  (:require [clojure.tools.logging :as log]
            [datascript.core :as d]
            [ring.adapter.jetty9 :as jetty]))

(defn get-client [db client-id]
  (d/entity db [:client-id client-id]))

(defn add-client! [conn {:keys [client-id] :as m}]
  (if-not (get-client @conn client-id)
    (do
      (log/debugf "Adding client: %s" client-id)
      (d/transact! conn [m]))
    (log/debugf "Client '%s' already exists. Not adding." client-id)))

(defn remove-client! [conn client-id]
  (when (get-client @conn client-id)
    (log/debugf "Removing client: %s" client-id)
    (d/transact! conn [[:db/retractEntity [:client-id client-id]]])))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :ws [{:keys [client-id ws]} message]
  (log/infof "Sending to %s via ws." client-id)
  (jetty/send! ws message))

(defn broadcast! [db client-ids message]
  (doseq [client-id client-ids :let [client (get-client db client-id)] :when client]
    (send! client message)))