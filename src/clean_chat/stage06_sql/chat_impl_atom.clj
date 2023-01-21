(ns clean-chat.stage06-sql.chat-impl-atom
  (:require [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.queries-datascript :as queries]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [datascript.core :as d]
            [integrant.core :as ig]))

(defrecord DatascriptChat [ctx])

(defn update-db [{:keys [ctx]} fn & args]
  (apply swap! ctx update :db fn args))

(defn update-outbox [{:keys [ctx]} fn & args]
  (apply swap! ctx update :outbox fn args))

(extend-type DatascriptChat
  chat-api/IChatEvents
  (create-message! [this message]
    (pp/pprint [:message message])
    (update-db this queries/create-message message))
  (join-chat! [this m]
    (update-db this queries/join-chat m))
  (leave-chat! [this m]
    (update-db this queries/leave-room m))
  (create-room! [this {:keys [room-name]}]
    (update-db this queries/insert-room room-name))
  (enter-room! [this {:keys [_username _room-name] :as m}]
    (update-db this queries/enter-room m))
  (leave-room! [this {:keys [_username] :as m}]
    (update-db this queries/leave-room m))
  (rename-room! [this {:keys [_old-room-name _new-room-name] :as m}]
    (update-db this queries/rename-room m))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [ctx]}]
    (queries/occupied-rooms (:db @ctx)))
  (all-active-users [{:keys [ctx]}]
    (queries/all-active-users (:db @ctx)))
  (users-in-room [{:keys [ctx]} room-name]
    (queries/users-in-room (:db @ctx) room-name))
  (current-room-name [{:keys [ctx]} username]
    (queries/current-room-name (:db @ctx) username))
  (room [{:keys [ctx]} room-name]
    (queries/room (:db @ctx) room-name))
  (chat-history [{:keys [ctx]} room-name]
    (queries/chat-history (:db @ctx) room-name))
  planex-api/IOutbox
  (outbox-write! [this evt]
    (update-outbox this conj evt))
  (outbox-read [{:keys [ctx]}]
    (:outbox @ctx))
  (outbox-get [this {to-find :uuid}]
    (update-outbox this #(first (filter (fn [{:keys [uuid]}] (= to-find uuid)) %))))
  (outbox-delete! [this {to-be-removed :uuid}]
    (update-outbox
     this
     #(vec (remove (fn [{:keys [uuid]}] (= to-be-removed uuid)) %))))
  planex-api/IReducer
  (plan-and-execute! [this command]
    (let [planned-events (planex-api/generate-plan this command)]
      (log/debugf
       "\nPLAN:\n%s" (with-out-str (pp/pprint planned-events)))
      (planex-api/execute-events! this planned-events))))

(defmethod ig/init-key ::atom-chat [_ initial-value]
  (log/debug "Creating atom-chat")
  (->DatascriptChat (atom initial-value)))

(def config
  {::atom-chat {:db     (d/empty-db queries/chat-schema)
                :outbox []}})
