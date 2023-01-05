(ns clean-chat.stage04-isolate-notifications.broker-test(:require [clojure.test :refer :all])
  (:require
    [clean-chat.stage04-isolate-notifications.client-api :as client-api]
    [clean-chat.stage04-isolate-notifications.test-client-api]
    [clean-chat.stage04-isolate-notifications.system :as config]
    [clean-chat.stage04-isolate-notifications.broker :as broker]
    [clean-chat.stage04-isolate-notifications.test-notifications]
    [clean-chat.stage04-isolate-notifications.events :refer :all]
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



