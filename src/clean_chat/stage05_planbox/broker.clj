(ns clean-chat.stage05-planbox.broker
  (:require [clean-chat.stage05-planbox.chat-command-planner]
            [clean-chat.stage05-planbox.chat-plan-executor]
            [clean-chat.stage05-planbox.client-api :as client-api]
            [clean-chat.stage05-planbox.htmx-events]
            [clean-chat.stage05-planbox.planex-api :as planex-api]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defn plan-and-execute [state command]
  (let [planned-events (planex-api/generate-plan state command)]
    (-> (reduce planex-api/execute-plan! state planned-events)
        (update :outbox into planned-events))))

(defn plan-and-execute! [{:keys [conn]} command]
  ;; The following block *must* be transactional!!
  (try
    (swap! conn plan-and-execute command)
    (catch Exception e
      (log/warnf
       "Command rejected! (%s)\n%s"
       (.getMessage e)
       (with-out-str (pp/pprint command))))))

(defn process-command [{:keys [clients conn] :as context} command]
  (let [{:keys [db outbox]} (plan-and-execute! context command)]
    (doseq [[transform clients-by-transform] (client-api/clients-by-transform clients)
            event outbox
            :let [ctx {:clients clients-by-transform :db db :transform transform}]]
      (planex-api/dispatch-event ctx event))
    (let [fltr (set outbox)]
      (swap! conn update :outbox (fn [ob] (vec (remove fltr ob)))))))
