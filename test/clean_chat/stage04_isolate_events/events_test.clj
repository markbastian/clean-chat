(ns clean-chat.stage04-isolate-events.events-test
  (:require [clojure.test :refer :all])
  (:require [clojure.test :refer :all]))

(deftest join-chat-commands-test
  (testing "Correct commands behavior when joining chat"
    (let [events (commands/dispatch-command
                  {:conn (d/create-conn config/chat-schema)}
                  {:command   :join-chat
                   :username  "Mark"
                   :room-name "public"})]
      (is (= [{:event :join-chat :username "Mark"}
              {:event :create-room :room-name "public"}
              {:event :enter-room :room-name "public" :username "Mark"}]
             events)))))

(deftest leave-chat-commands-test
  (testing "Correct commands behavior when joining chat"
    (let [conn (d/create-conn config/chat-schema)
          join-events (commands/dispatch-command
                       {:conn conn}
                       {:command   :join-chat
                        :username  "Mark"
                        :room-name "public"})
          leave-events (commands/dispatch-command
                        {:conn conn}
                        {:command  :leave-chat
                         :username "Mark"})]
      (is (= [{:event :join-chat :username "Mark"}
              {:event :create-room :room-name "public"}
              {:event :enter-room :room-name "public" :username "Mark"}]
             join-events))
      (is (= [{:event :leave-room, :room-name "public" :username "Mark"}
              {:event :leave-chat, :username "Mark"}]
             leave-events))))
  (testing "Leaving without being present is a null-op"
    (let [conn (d/create-conn config/chat-schema)
          events (commands/dispatch-command
                  {:conn conn}
                  {:command  :leave-chat
                   :username "Mark"})]
      (is (nil? events)))))
