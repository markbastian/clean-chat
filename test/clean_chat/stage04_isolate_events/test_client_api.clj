(ns clean-chat.stage04-isolate-events.test-client-api
  (:require [clean-chat.stage04-isolate-events.client-api :as client-api]
            [clojure.test :refer :all]))

(defmethod client-api/send! :test [{:keys [res]} message]
  (swap! res (fnil conj []) message))
