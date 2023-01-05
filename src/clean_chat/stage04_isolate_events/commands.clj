(ns clean-chat.stage04-isolate-events.commands
  (:require
    [clean-chat.stage04-isolate-events.domain :as domain]
    [clojure.pprint :as pp]
    [clojure.tools.logging :as log]))

(defmulti dispatch-command (fn [_ctx {:keys [command]}] command))

(defmethod dispatch-command :default [_ {:keys [command] :as cmd}]
  (let [cmdstr (with-out-str (pp/pprint cmd))]
    (log/warnf "Unhandled command: %s\n%s" command cmdstr)))

(defmethod dispatch-command :chat-message
  [context {:keys [username chat-message]}]
  (domain/create-chat-message! context username chat-message))

(defmethod dispatch-command :change-room [context command]
  (domain/join-room! context command))

(defmethod dispatch-command :rename-room [context command]
  (domain/rename-room! context command))

(defmethod dispatch-command :join-chat [context command]
  (domain/join-chat! context command))

(defmethod dispatch-command :leave-chat [context {:keys [username]}]
  (domain/leave-chat! context username))