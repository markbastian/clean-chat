(ns clean-chat.stage06-sql.broker-atom
  (:require [clean-chat.stage06-sql.client-manager :as client-api]
            [clean-chat.stage06-sql.htmx-events]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.planex-chat]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defn plan-and-execute [state command]
  (let [planned-events (planex-api/generate-plan state command)]
    (reduce (fn [acc evt]
              (-> acc
                  (planex-api/execute-plan! evt)
                  (planex-api/outbox-write! evt)))
            state
            planned-events)))

(defn plan-and-execute! [{:keys [conn]} command]
  ;; The following block *must* be transactional!!
  (try
    (swap! conn plan-and-execute command)
    (catch Exception e
      (log/warnf
       "Command rejected! (%s)\n%s"
       (.getMessage e)
       (with-out-str (pp/pprint command))))))

(defn handle-events [{:keys [clients conn]}]
  (let [cbt (client-api/clients-by-transform clients)
        events (planex-api/outbox-read conn)]
    (doseq [[transform clients-by-transform] cbt
            event events
            :let [ctx (into conn {:clients   clients-by-transform
                                  :transform transform})]]
      (planex-api/dispatch-event ctx event)
      (planex-api/outbox-delete! conn event))))

;; TODO!!!!! GENERALIZE AND FIX FOR SQLLITE
;; We must hide the fact that the outbox is a key (make it an api)
;; and we need to make conn not be an atom (again, api)
;; and (aside) the sql chat api isn't transactional :( UGH
(defn process-command [{:keys [clients conn] :as context} command]
  (let [{:keys [outbox]} (plan-and-execute! context command)]
    (doseq [[transform clients-by-transform] (client-api/clients-by-transform clients)
            event outbox
            :let [ctx (into @conn {:clients clients-by-transform :transform transform})]]
      (planex-api/dispatch-event ctx event))
    (let [fltr (set outbox)]
      (swap! conn update :outbox (fn [ob] (vec (remove fltr ob)))))))
