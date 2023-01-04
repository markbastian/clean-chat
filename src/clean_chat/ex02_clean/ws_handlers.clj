(ns clean-chat.ex02-clean.ws-handlers
  (:require [clean-chat.ex02-clean.domain :as domain]
            [clean-chat.ex02-clean.htmx-notifications :as htmx-notifications]
            [clean-chat.utils :as u]
            [clojure.tools.logging :as log]
            [datascript.core :as d]))

(defn on-connect [{:keys [title path-params conn] :as context} ws]
  (let [{:keys [username room-name]} path-params]
    (if-not (:ws (d/entity @conn [:username username]))
      (domain/join-room! context {:username  username
                                  :room-name room-name
                                  :ws        ws})
      (htmx-notifications/notify-and-close-login-failure title ws))))

(defn on-text [{:keys [path-params conn] :as context} _ws text-message]
  (let [{:keys [username]} path-params
        {:keys [room-name chat-message] :as json} (u/read-json text-message)
        command (keyword (get-in json [:HEADERS :HX-Trigger-Name]))]
    (log/infof "Dispatching command '%s'" command)
    (case command
      :chat-message (domain/create-chat-message! context username chat-message)
      :change-room (domain/join-room! context {:username  username
                                               :room-name room-name})
      (log/warnf "Unhandled command '%s'" command))))

(defn on-close [{:keys [path-params] :as context} _ws _status-code _reason]
  (let [{:keys [username]} path-params]
    (log/debugf "on-close triggered for user: %s" username)
    (domain/leave-chat! context username)))

(defn on-error [{:keys [path-params] :as context} ws err]
  (let [{:keys [username]} path-params]
    (log/debugf "on-error triggered for user: %s" username)
    (domain/leave-chat! context username)
    (println ws)
    (println err)
    (println "ERROR")))