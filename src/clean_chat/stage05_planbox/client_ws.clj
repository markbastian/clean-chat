(ns clean-chat.stage05-planbox.client-ws
  (:require [clean-chat.stage05-planbox.client-api :as client-api]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as jetty]))

(defmethod client-api/send! :ws [{:keys [client-id ws]} message]
  (log/tracef "Sending %s to %s via ws." message client-id)
  (jetty/send! ws message))
