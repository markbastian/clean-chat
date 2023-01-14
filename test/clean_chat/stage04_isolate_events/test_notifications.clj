(ns clean-chat.stage04-isolate-events.test-notifications
  (:require [clean-chat.stage04-isolate-events.client-api :as client-api]
            [clean-chat.stage04-isolate-events.events :as events]
            [clojure.test :refer :all]))

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
