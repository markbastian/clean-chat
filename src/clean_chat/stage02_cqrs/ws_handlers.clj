(ns clean-chat.stage02-cqrs.ws-handlers
  (:require [clean-chat.stage02-cqrs.commands :as commands]
            [clean-chat.stage02-cqrs.htmx-notifications :as htmx-notifications]
            [clean-chat.utils :as u]
            [clojure.tools.logging :as log]
            [datascript.core :as d]))

(defn on-connect [{:keys [title path-params conn] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if-not (:ws (d/entity @conn [:username username]))
      (commands/dispatch-command context {:command   :change-room
                                          :username  username
                                          :room-name room-name
                                          :ws        ws})
      (htmx-notifications/notify-and-close-login-failure title ws))))

(defn on-text [{:keys [path-params] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        {:keys [HEADERS] :as json} (u/read-json text-message)
        command (-> json
                    (assoc
                     :username username
                     :command (some-> HEADERS :HX-Trigger-Name keyword))
                    (dissoc :HEADERS))]
    (commands/dispatch-command context command)))

(defn on-close [{:keys [path-params] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params]
    (log/debugf "on-close triggered for user: %s" username)
    (let [command {:command :leave-chat :username username}]
      (commands/dispatch-command context command))))

(defn on-error [{:keys [path-params] :as context} ws err]
  (let [{:keys [username]} path-params]
    (log/debugf "on-error triggered for user: %s" username)
    (let [command {:command :leave-chat :username username}]
      (commands/dispatch-command context command))
    (println ws)
    (println err)
    (println "ERROR")))
