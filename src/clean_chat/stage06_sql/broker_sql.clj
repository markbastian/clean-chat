(ns clean-chat.stage06-sql.broker-sql
  (:require [clean-chat.stage06-sql.chat-command-planner]
            [clean-chat.stage06-sql.chat-plan-executor]
            [clean-chat.stage06-sql.client-manager :as client-api]
            [clean-chat.stage06-sql.htmx-events]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [next.jdbc :as jdbc]))

(defn plan-and-execute! [{:keys [conn]} command]
  (jdbc/with-transaction [tx (:db conn)]
    (let [txconn ((assoc conn :db tx))
          planned-events (planex-api/generate-plan txconn command)]
      (doseq [event planned-events]
        (planex-api/execute-plan! txconn event)))))

(defn handle-events [{:keys [clients conn]}]
  (let [cbt (client-api/clients-by-transform clients)
        events (planex-api/outbox-read conn)]
    (doseq [[transform clients-by-transform] cbt
            event events
            :let [ctx (into conn {:clients   clients-by-transform
                                  :transform transform})]]
      (planex-api/dispatch-event ctx event)
      (planex-api/outbox-delete! conn event))))

(defn process-command [context command]
  (plan-and-execute! context command)
  (handle-events context))