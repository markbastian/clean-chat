(ns clean-chat.stage06-sql.command-event-ref-test
  (:require
   [clean-chat.stage06-sql.api.reducer :as reducer-api]
   [clean-chat.stage06-sql.chat-impl-atom :as cid]
   [clean-chat.stage06-sql.chat-impl-ref :as cir]
   [clean-chat.stage06-sql.chat-impl-sqlite :as cis]
   [clean-chat.stage06-sql.chat-reducer]
   [clean-chat.stage06-sql.datascript-schema :as datascript-schema]
   [clean-chat.system :refer [with-system]]
   [clojure.test :refer [deftest is testing]]
   [datascript.core :as d]))

(def join-room-event-keys [:event :username :room-name])
(def chat-event-keys [:event :username :room-name :message])
(def rename-room-event-keys [:event :old-room-name :new-room-name])
(def leave-chat-event-keys [:event :room-name :username])

(defn command-lifecycle-test [ctx]
  (let [join-command        {:command   :join-chat
                             :username  "Mark"
                             :room-name "public"}
        join-events         (reducer-api/generate-plan ctx join-command)
        _                   (reducer-api/execute-events! ctx join-events)
        chat-command        {:command      :chat-message
                             :username     "Mark"
                             :chat-message "Hi!"}
        chat-events         (reducer-api/generate-plan ctx chat-command)
        _                   (reducer-api/execute-events! ctx chat-events)
        rename-room-command {:command   :rename-room
                             :username  "Mark"
                             :room-name "private"}
        rename-room-events  (reducer-api/generate-plan ctx rename-room-command)
        _                   (reducer-api/execute-events! ctx rename-room-events)
        leave-chat-command  {:command  :leave-chat
                             :username "Mark"}
        leave-chat-events   (reducer-api/generate-plan ctx leave-chat-command)
        _                   (reducer-api/execute-events! ctx leave-chat-events)]
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
    (is (= 7 (count (reducer-api/outbox-read ctx))))
    (doseq [outbox-event (take 1 (reducer-api/outbox-read ctx))]
      (reducer-api/outbox-delete! ctx outbox-event))))

(deftest sql-command-lifecycle-test
  (testing "Testing sql implementation of the command lifecycle"
    (with-system [system cis/config]
      (command-lifecycle-test (::cis/sql-chat system)))))

(deftest ref-command-lifecycle-test
  (testing "Test the lifecycle of events"
    (dosync
     (let [ctx (cir/map->RefChat {:db     (ref (d/empty-db datascript-schema/chat-schema))
                                  :outbox (ref [])})]
       (command-lifecycle-test ctx)))))

(deftest atom-command-lifecycle-test
  (testing "Test the lifecycle of events"
    (let [ctx (cid/->DatascriptChat
               (atom
                {:db     (d/empty-db datascript-schema/chat-schema)
                 :outbox []}))]
      (command-lifecycle-test ctx))))
