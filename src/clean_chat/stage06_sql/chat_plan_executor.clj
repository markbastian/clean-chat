(ns clean-chat.stage06-sql.chat-plan-executor
  (:require
   [clean-chat.stage06-sql.chat-api :as chat-api]
   [clean-chat.stage06-sql.planex-api :as planex-api]))

(defmethod planex-api/execute-plan! :create-message
  [state event]
  (chat-api/create-message! state event))

(defmethod planex-api/execute-plan! :join-chat [state event]
  (chat-api/join-chat! state event))

(defmethod planex-api/execute-plan! :leave-chat [state event]
  (chat-api/leave-chat! state event))

(defmethod planex-api/execute-plan! :create-room [state event]
  (chat-api/create-room! state event))

(defmethod planex-api/execute-plan! :enter-room [state event]
  (chat-api/enter-room! state event))

(defmethod planex-api/execute-plan! :leave-room [state event]
  (chat-api/leave-room! state event))

(defmethod planex-api/execute-plan! :rename-room [state event]
  (chat-api/rename-room! state event))
