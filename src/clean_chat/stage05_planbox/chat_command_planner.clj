(ns clean-chat.stage05-planbox.chat-command-planner
  (:require
   [clean-chat.stage05-planbox.planex-api :as planex-api]
   [clean-chat.stage05-planbox.queries :as queries]
   [clojure.tools.logging :as log]))

(defmethod planex-api/generate-plan :chat-message
  [{:keys [db]} {:keys [username chat-message]}]
  (if-some [room-name (queries/current-room-name db username)]
    [{:event     :create-message
      :username  username
      :room-name room-name
      :message   chat-message}]
    (log/debugf "'%s' is not in a room." username)))

(defmethod planex-api/generate-plan :change-room
  [{:keys [db]} {:keys [username room-name]}]
  (when-some [old-room-name (queries/current-room-name db username)]
    (when-not (= room-name old-room-name)
      (cond-> []
        old-room-name
        (conj {:event :leave-room :room-name old-room-name :username username})
        (not (queries/room-exists? db room-name))
        (conj {:event :create-room :room-name room-name})
        true
        (conj {:event :enter-room :room-name room-name :username username})))))

(defmethod planex-api/generate-plan :rename-room
  [{:keys [db]} {:keys [username room-name]}]
  (when-not (queries/room-exists? db room-name)
    (let [old-room-name (queries/current-room-name db username)
          id (:db/id (queries/room db old-room-name))]
      (when (and id (not= room-name old-room-name))
        [{:event         :rename-room
          :old-room-name old-room-name
          :new-room-name room-name}]))))

(defmethod planex-api/generate-plan :join-chat
  [{:keys [db]} {:keys [username room-name]}]
  (when-not (queries/current-room-name db username)
    (cond-> [{:event :join-chat :username username}]
      (not (queries/room-exists? db room-name))
      (conj {:event :create-room :room-name room-name})
      true
      (conj {:event :enter-room :room-name room-name :username username}))))

(defmethod planex-api/generate-plan :leave-chat [{:keys [db]} {:keys [username]}]
  (when-some [room-name (queries/current-room-name db username)]
    [{:event :leave-room :room-name room-name :username username}
     {:event :leave-chat :username username}]))
