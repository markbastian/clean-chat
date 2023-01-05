(ns clean-chat.ex04-isolate-notifications.broker
  (:require
    [clean-chat.ex04-isolate-notifications.client-api :as client-api]
    [clean-chat.ex04-isolate-notifications.commands :as commands]
    [clean-chat.ex04-isolate-notifications.events :as events]))

(defn process-command [{:keys [clients conn] :as context} command]
  (let [events (commands/dispatch-command context command)]
    (doseq [[transform clients] (client-api/clients-by-transform @clients)
            event events
            :let [ctx {:clients clients :db @conn :transform transform}]]
      (println [event transform clients])
      (events/dispatch-event ctx event))))
