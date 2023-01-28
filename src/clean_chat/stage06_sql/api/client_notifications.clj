(ns clean-chat.stage06-sql.api.client-notifications
  (:require [clojure.tools.logging :as log]))

(defmulti notify-client (fn [{:keys [transform]} {:keys [event]}]
                          [transform event]))

(defmethod notify-client :default [{:keys [transform]}
                                   {:keys [event]}]
  (log/warnf "Unhandled dispatch value: [%s %s]" transform event))
