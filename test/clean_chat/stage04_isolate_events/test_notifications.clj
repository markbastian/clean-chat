(ns clean-chat.stage04-isolate-notifications.test-notifications
  (:require [clojure.test :refer :all]
            [clean-chat.stage04-isolate-notifications.events :as events]
            [clean-chat.stage04-isolate-notifications.client-api :as client-api]))

(defn echo-message [{:keys [clients]} event]
  (doseq [client (client-api/all-clients clients)]
    (client-api/send! client event)))

(defmethod events/dispatch-event [:test :join-chat] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :leave-chat] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :create-message] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :enter-room] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :leave-room] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :create-room] [ctx event]
  (echo-message ctx event))
(defmethod events/dispatch-event [:test :rename-room] [ctx event]
  (echo-message ctx event))



