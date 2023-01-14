(ns clean-chat.stage05-planbox.planex-api
  (:require [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defmulti generate-plan (fn [_ctx {:keys [command]}] command))

(defmethod generate-plan :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmulti execute-plan! (fn [_context {:keys [event]}] event))

(defmethod execute-plan! :default [_ event]
  (log/warnf "Unable to apply plan: %s" (:event event))
  (with-out-str (pp/pprint event)))

(defmulti dispatch-event (fn [{:keys [transform]} {:keys [event]}]
                           [transform event]))

(defmethod dispatch-event :default [{:keys [transform]}
                                    {:keys [event]}]
  (log/warnf "Unhandled dispatch value: [%s %s]" transform event))
