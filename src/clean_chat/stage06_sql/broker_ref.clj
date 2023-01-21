(ns clean-chat.stage06-sql.broker-ref
  (:require [clean-chat.stage06-sql.client-api :as client-api]
            [clean-chat.stage06-sql.htmx-events]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.planex-chat]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defn handle-events [{:keys [clients conn]}]
  (dosync
   (let [cbt    (client-api/clients-by-transform clients)
         events (planex-api/outbox-read conn)]
     (doseq [[transform clients-by-transform] cbt
             event events
             :let [ctx (into conn {:clients   clients-by-transform
                                   :transform transform})]]
       (log/debugf
        "\nDISPATCHING:\n%s" (with-out-str (pp/pprint event)))
       (planex-api/dispatch-event ctx event)
       (log/debugf
        "\nDELETING:\n%s" (with-out-str (pp/pprint event)))
       (planex-api/outbox-delete! conn event)))))

(defn process-command [{:keys [conn] :as context} command]
  (planex-api/plan-and-execute! conn command)
  (handle-events context))
