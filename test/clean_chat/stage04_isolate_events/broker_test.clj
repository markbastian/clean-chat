(ns clean-chat.stage04-isolate-events.broker-test
  (:require
   [clean-chat.stage04-isolate-events.broker :as broker]
   [clean-chat.stage04-isolate-events.client-api :as client-api]
   [clean-chat.stage04-isolate-events.system :as config]
   [clojure.test :refer [deftest is testing]]
   [datascript.core :as d]))

(deftest join-chat-broker-test
  (testing "Correct broker behavior when joining chat"
    (let [clients (atom {})
          res (atom [])
          _ (client-api/add-client! clients {:client-id "Mark"
                                             :transport :test
                                             :res       res
                                             :transform :test})
          _ (broker/process-command
             {:conn    (d/create-conn config/chat-schema)
              :clients clients}
             {:command   :join-chat
              :username  "Mark"
              :room-name "public"})]
      (is (= [{:event :join-chat :username "Mark"}
              {:event :create-room :room-name "public"}
              {:event :enter-room :room-name "public" :username "Mark"}]
             @res)))))
