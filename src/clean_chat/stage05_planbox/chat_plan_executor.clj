(ns clean-chat.stage05-planbox.chat-plan-executor
  (:require
    [clean-chat.stage05-planbox.queries :as queries]
    [clean-chat.stage05-planbox.planex-api :as planex-api]
    [datascript.core :as d]))

(defmethod planex-api/execute-plan! :create-message
  [state {:keys [message username room-name]}]
  (update state :db d/db-with
          [{:message                message
            :nanos-since-unix-epoch (System/nanoTime)
            :user                   {:username username}
            :room                   {:room-name room-name}}]))

(defmethod planex-api/execute-plan! :join-chat
  [state {:keys [username]}]
  (update state :db d/db-with [{:username username}]))

(defmethod planex-api/execute-plan! :leave-chat
  [state {:keys [username]}]
  (update state :db d/db-with [[:db.fn/retractAttribute [:username username] :room]]))

(defmethod planex-api/execute-plan! :create-room
  [state {:keys [room-name]}]
  (update state :db d/db-with [{:room-name room-name}]))

(defmethod planex-api/execute-plan! :enter-room
  [state {:keys [username room-name]}]
  (update state :db d/db-with [{:username username :room {:room-name room-name}}]))

(defmethod planex-api/execute-plan! :leave-room
  [state {:keys [username]}]
  (update state :db d/db-with [[:db.fn/retractAttribute [:username username] :room]]))

(defmethod planex-api/execute-plan! :rename-room
  [{:keys [db] :as state} {:keys [old-room-name new-room-name]}]
  (let [id (:db/id (queries/room db old-room-name))]
    (let [tx-data [[:db/add id :room-name new-room-name]]]
      (update state :db d/db-with tx-data))))
