(ns clean-chat.stage06-sql.chat-impl-atom
  (:require [clean-chat.stage06-sql.api.reducer :as reducer-api]
            [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.commands-datascript :as datascript-commands]
            [clean-chat.stage06-sql.datascript-schema :as datascript-schema]
            [clean-chat.stage06-sql.queries-datascript :as datascript-queries]
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
    (update-db this datascript-commands/create-message message))
  (join-chat! [this m]
    (update-db this datascript-commands/join-chat m))
  (leave-chat! [this m]
    (update-db this datascript-commands/leave-room m))
  (create-room! [this {:keys [room-name]}]
    (update-db this datascript-commands/insert-room room-name))
  (enter-room! [this {:keys [_username _room-name] :as m}]
    (update-db this datascript-commands/enter-room m))
  (leave-room! [this {:keys [_username] :as m}]
    (update-db this datascript-commands/leave-room m))
  (rename-room! [this {:keys [_old-room-name _new-room-name] :as m}]
    (update-db this datascript-commands/rename-room m))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [ctx]}]
    (datascript-queries/occupied-rooms (:db @ctx)))
  (all-active-users [{:keys [ctx]}]
    (datascript-queries/all-active-users (:db @ctx)))
  (users-in-room [{:keys [ctx]} room-name]
    (datascript-queries/users-in-room (:db @ctx) room-name))
  (current-room-name [{:keys [ctx]} username]
    (datascript-queries/current-room-name (:db @ctx) username))
  (room [{:keys [ctx]} room-name]
    (datascript-queries/room (:db @ctx) room-name))
  (chat-history [{:keys [ctx]} room-name]
    (datascript-queries/chat-history (:db @ctx) room-name))
  reducer-api/IOutbox
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
  reducer-api/IReducer
  (plan-and-execute! [this command]
    (let [planned-events (reducer-api/generate-plan this command)]
      (log/debugf
       "\nPLAN:\n%s" (with-out-str (pp/pprint planned-events)))
      (reducer-api/execute-events! this planned-events))))

(defmethod ig/init-key ::atom-chat [_ initial-value]
  (log/debug "Creating atom-chat")
  (->DatascriptChat (atom initial-value)))

(def config
  {::atom-chat {:db     (d/empty-db datascript-schema/chat-schema)
                :outbox []}})
