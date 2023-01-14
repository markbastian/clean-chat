(ns clean-chat.stage04-isolate-events.domain
  (:require
   [clean-chat.stage04-isolate-events.queries :as queries]
   [datascript.core :as d]))

(defn create-chat-message! [{:keys [conn]} username message]
  (let [room-name (queries/current-room-name @conn username)]
    (d/transact! conn [{:message                message
                        :nanos-since-unix-epoch (System/nanoTime)
                        :user                   {:username username}
                        :room                   {:room-name room-name}}])
    [{:event     :create-message
      :username  username
      :room-name room-name
      :message   message}]))

(defn join-chat! [{:keys [conn]} {:keys [username room-name]}]
  (when-not (queries/current-room-name @conn username)
    (let [room-will-be-created? (not (queries/room-exists? @conn room-name))
          tx-data [{:username username
                    :room     {:room-name room-name}}]
          _ (d/transact! conn tx-data)]
      (cond-> [{:event :join-chat :username username}]
        room-will-be-created?
        (conj {:event :create-room :room-name room-name})
        true
        (conj {:event :enter-room :room-name room-name :username username})))))

(defn join-room! [{:keys [conn]} {:keys [username room-name]}]
  (when-some [old-room-name (queries/current-room-name @conn username)]
    (when-not (= room-name old-room-name)
      (let [room-will-be-created? (not (queries/room-exists? @conn room-name))
            tx-data [{:username username
                      :room     {:room-name room-name}}]
            _ (d/transact! conn tx-data)]
        (cond-> []
          old-room-name
          (conj {:event :leave-room :room-name old-room-name :username username})
          room-will-be-created?
          (conj {:event :create-room :room-name room-name})
          true
          (conj {:event :enter-room :room-name room-name :username username}))))))

(defn leave-chat! [{:keys [conn]} username]
  (when-some [room-name (queries/current-room-name @conn username)]
    (let [tx-data [[:db.fn/retractAttribute [:username username] :room]]
          _ (d/transact! conn tx-data)]
      [{:event :leave-room :room-name room-name :username username}
       {:event :leave-chat :username username}])))

(defn rename-room! [{:keys [conn]} {:keys [username room-name]}]
  (when-not (queries/room-exists? @conn room-name)
    (let [old-room-name (queries/current-room-name @conn username)
          id (:db/id (queries/room @conn old-room-name))]
      (when (and id (not= room-name old-room-name))
        (let [tx-data [[:db/add id :room-name room-name]]
              _ (d/transact! conn tx-data)]
          [{:event         :rename-room
            :old-room-name old-room-name
            :new-room-name room-name}])))))

;; Refactorings
;; - Convert all notifications to events
