(ns clean-chat.stage06-sql.client-manager
  (:require
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]))

(defn get-client [clients client-id]
  (get-in clients [:client-id client-id]))

(defn clients-by-transform [clients]
  (get clients :transforms))

(defn all-clients [clients]
  (vals (get clients :client-id)))

(defn add-client [clients {:keys [client-id transform] :as client}]
  (-> clients
      (assoc-in [:client-id client-id] client)
      (assoc-in [:transforms transform :client-id client-id] client)))

(defn remove-client [clients client-id]
  (let [{:keys [transform]} (get-client clients client-id)]
    (-> clients
        (update :client-id dissoc client-id)
        (update-in [:transforms transform :client-id] dissoc client-id))))

(defn add-client! [client-manager {:keys [client-id] :as client}]
  (if-not (get-client @client-manager client-id)
    (do
      (log/debugf "Adding client: %s" client-id)
      (swap! client-manager add-client client))
    (log/debugf "Client '%s' already exists. Not adding." client-id)))

(defn remove-client! [client-manager client-id]
  (when (get-client @client-manager client-id)
    (log/debugf "Removing client: %s" client-id)
    (swap! client-manager remove-client client-id)))

(defmulti send! (fn [{:keys [transport] :as _user} _message] transport))

(defmethod send! :default [_ message]
  (log/warnf
   "No client found to send message:\n%s"
   (with-out-str (pp/pprint message))))

(defn broadcast! [db client-ids message]
  (doseq [client-id client-ids :let [client (get-client db client-id)] :when client]
    (send! client message)))
