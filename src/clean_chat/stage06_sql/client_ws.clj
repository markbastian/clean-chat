(ns clean-chat.stage06-sql.client-ws
  (:require [clean-chat.stage06-sql.client-manager :as client-api]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defmethod client-api/send! :ws [{:keys [client-id ws]} message]
  (log/tracef "Sending %s to %s via ws." message client-id)
  (jetty/send! ws message))