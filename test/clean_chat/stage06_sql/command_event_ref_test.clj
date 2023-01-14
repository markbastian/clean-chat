(ns clean-chat.stage06-sql.command-event-ref-test
  (:require [clojure.test :refer :all])
  (:require [clean-chat.stage06-sql.chat-impl-ref :as cir]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.queries-datascript :as queries]
            [clean-chat.stage06-sql.chat-impl-ref]
            [datascript.core :as d]))

(def join-room-event-keys [:event :username :room-name])
(def chat-event-keys [:event :username :room-name :message])
(def rename-room-event-keys [:event :old-room-name :new-room-name])
(def leave-chat-event-keys [:event :room-name :username])

(deftest rename-room-command-test
  (testing "A :rename-room event results in the right events"
    (dosync
     (let [ctx (cir/map->RefChat {:db     (ref (d/empty-db queries/chat-schema))
                                  :outbox (ref [])})
           join-command {:command   :join-chat
                         :username  "Mark"
                         :room-name "public"}
           join-events (planex-api/generate-plan ctx join-command)
           _ (planex-api/execute-events! ctx join-events)
           chat-command {:command      :chat-message
                         :username     "Mark"
                         :chat-message "Hi!"}
           chat-events (planex-api/generate-plan ctx chat-command)
           _ (planex-api/execute-events! ctx chat-events)
           rename-room-command {:command   :rename-room
                                :username  "Mark"
                                :room-name "private"}
           rename-room-events (planex-api/generate-plan ctx rename-room-command)
           _ (planex-api/execute-events! ctx rename-room-events)
           leave-chat-command {:command  :leave-chat
                               :username "Mark"}
           leave-chat-events (planex-api/generate-plan ctx leave-chat-command)
           _ (planex-api/execute-events! ctx leave-chat-events)]
       (is (= [{:event :join-chat :username "Mark"}
               {:event :create-room :room-name "public"}
               {:event :enter-room :room-name "public" :username "Mark"}]
              (mapv #(select-keys % join-room-event-keys) join-events)))
       (is (= [{:event     :create-message
                :username  "Mark"
                :room-name "public"
                :message   "Hi!"}]
              (mapv #(select-keys % chat-event-keys) chat-events)))
       (is (= [{:event         :rename-room
                :old-room-name "public"
                :new-room-name "private"}]
              (mapv #(select-keys % rename-room-event-keys) rename-room-events)))
       (is (= [{:event :leave-room :room-name "private" :username "Mark"}
               {:event :leave-chat :username "Mark"}]
              (mapv #(select-keys % leave-chat-event-keys) leave-chat-events)))
       (is (= 7 (count (planex-api/outbox-read ctx))))
       (doseq [outbox-event (take 1 (planex-api/outbox-read ctx))]
         (planex-api/outbox-delete! ctx outbox-event))))))
