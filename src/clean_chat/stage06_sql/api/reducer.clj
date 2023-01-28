(ns clean-chat.stage06-sql.api.reducer
  (:require
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]))

;; Rule of thumb - IO/Stateful things belong in protocols
(defprotocol IOutbox
  (outbox-write! [this event])
  (outbox-read [this])
  (outbox-get [this event])
  (outbox-delete! [this event]))

;; Rule of thumb - Data processing/streams belong in multimethods
(defmulti generate-plan (fn [_ctx {:keys [command]}] command))

(defmethod generate-plan :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmulti execute-plan! (fn [_context {:keys [event]}] event))

(defmethod execute-plan! :default [_ event]
  (log/warnf "Unable to apply plan: %s" (:event event))
  (with-out-str (pp/pprint event)))

(defn execute-events! [ctx events]
  (doseq [event events]
    (log/debugf "\nEXECUTING:\n%s" (with-out-str (pp/pprint event)))
    (execute-plan! ctx event)
    (log/debugf "\nOUTBOXING:\n%s" (with-out-str (pp/pprint event)))
    (outbox-write! ctx event)
    (log/debug "OUTBOXED")))

(defprotocol IReducer
  (plan-and-execute! [_ command]))
