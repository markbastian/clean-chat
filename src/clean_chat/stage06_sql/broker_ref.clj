(ns clean-chat.stage06-sql.broker-ref
  (:require [clean-chat.stage06-sql.client-manager :as client-api]
            [clean-chat.stage06-sql.htmx-events]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.planex-chat]))

(defn plan-and-execute! [{:keys [conn]} command]
  ;; Needs to be in a protocol or mm as the transaction here may be different
  ;; per implementation.
  (dosync
   (let [planned-events (planex-api/generate-plan conn command)]
     (planex-api/execute-events! conn planned-events))))

(defn handle-events [{:keys [clients conn]}]
  (dosync
   (let [cbt (client-api/clients-by-transform clients)
         events (planex-api/outbox-read conn)]
     (doseq [[transform clients-by-transform] cbt
             event events
             :let [ctx (into conn {:clients   clients-by-transform
                                   :transform transform})]]
       (planex-api/dispatch-event ctx event)
       (planex-api/outbox-delete! conn event)))))

(defn process-command [context command]
  (plan-and-execute! context command)
  (handle-events context))
