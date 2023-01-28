(ns clean-chat.stage06-sql.broker-ref
  (:require [clean-chat.stage06-sql.api.client-notifications :as client-notifications]
            [clean-chat.stage06-sql.api.reducer :as reducer-api]
            [clean-chat.stage06-sql.chat-reducer]
            [clean-chat.stage06-sql.client-api :as client-api]
            [clean-chat.stage06-sql.client-notifications-htmx]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defn handle-events [{:keys [clients conn]}]
  (dosync
   (let [cbt    (client-api/clients-by-transform clients)
         events (reducer-api/outbox-read conn)]
     (doseq [[transform clients-by-transform] cbt
             event events
             :let [ctx (into conn {:clients   clients-by-transform
                                   :transform transform})]]
       (log/debugf
        "\nDISPATCHING:\n%s" (with-out-str (pp/pprint event)))
       (client-notifications/notify-client ctx event)
       (log/debugf
        "\nDELETING:\n%s" (with-out-str (pp/pprint event)))
       (reducer-api/outbox-delete! conn event)))))

(defn process-command [{:keys [conn] :as context} command]
  (reducer-api/plan-and-execute! conn command)
  (handle-events context))
