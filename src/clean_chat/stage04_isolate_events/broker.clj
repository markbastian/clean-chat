(ns clean-chat.stage04-isolate-events.broker
  (:require
    [clean-chat.stage04-isolate-events.client-api :as client-api]
    [clean-chat.stage04-isolate-events.commands :as commands]
    [clean-chat.stage04-isolate-events.events :as events]))

(defn process-command [{:keys [clients conn] :as context} command]
  (let [events (commands/dispatch-command context command)]
    (doseq [[transform clients] (client-api/clients-by-transform @clients)
            event events
            :let [ctx {:clients clients :db @conn :transform transform}]]
      (println [event transform clients])
      (events/dispatch-event ctx event))))
