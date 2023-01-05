(ns clean-chat.stage04-isolate-notifications.test-client-api
  (:require [clean-chat.stage04-isolate-notifications.client-api :as client-api]
            [clojure.test :refer :all]))

(defmethod client-api/send! :test [{:keys [res]} message]
  (swap! res (fnil conj []) message))