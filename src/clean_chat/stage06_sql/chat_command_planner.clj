(ns clean-chat.stage06-sql.chat-command-planner
  (:require
    [clean-chat.stage06-sql.planex-api :as planex-api]
    [clean-chat.stage06-sql.chat-api :as chat-api]
    [clojure.tools.logging :as log]))

(defmethod planex-api/generate-plan :chat-message
  [context {:keys [username chat-message]}]
  (if-some [room-name (chat-api/current-room-name context username)]
    [{:event     :create-message
      :username  username
      :room-name room-name
      :message   chat-message}]
    (log/debugf "'%s' is not in a room." username)))

(defmethod planex-api/generate-plan :change-room
  [context {:keys [username room-name]}]
  (when-some [old-room-name (chat-api/current-room-name context username)]
    (when-not (= room-name old-room-name)
      (cond-> []
              old-room-name
              (conj {:event :leave-room :room-name old-room-name :username username})
              (not (chat-api/room context room-name))
              (conj {:event :create-room :room-name room-name})
              true
              (conj {:event :enter-room :room-name room-name :username username})))))

(defmethod planex-api/generate-plan :rename-room
  [context {:keys [username room-name]}]
  (when-not (chat-api/room context room-name)
    (let [old-room-name (chat-api/current-room-name context username)
          id (:db/id (chat-api/room context old-room-name))]
      (when (and id (not= room-name old-room-name))
        [{:event         :rename-room
          :old-room-name old-room-name
          :new-room-name room-name}]))))

(defmethod planex-api/generate-plan :join-chat
  [context {:keys [username room-name]}]
  (when-not (chat-api/current-room-name context username)
    (cond-> [{:event :join-chat :username username}]
            (not (chat-api/room context room-name))
            (conj {:event :create-room :room-name room-name})
            true
            (conj {:event :enter-room :room-name room-name :username username}))))

(defmethod planex-api/generate-plan :leave-chat [context {:keys [username]}]
  (when-some [room-name (chat-api/current-room-name context username)]
    [{:event :leave-room :room-name room-name :username username}
     {:event :leave-chat :username username}]))