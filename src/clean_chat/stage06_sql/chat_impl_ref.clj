(ns clean-chat.stage06-sql.chat-impl-ref
  (:require
   [clean-chat.stage06-sql.api.reducer :as reducer-api]
   [clean-chat.stage06-sql.chat-api :as chat-api]
   [clean-chat.stage06-sql.commands-datascript :as datascript-commands]
   [clean-chat.stage06-sql.datascript-schema :as datascript-schema]
   [clean-chat.stage06-sql.queries-datascript :as datascript-queries]
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]
   [datascript.core :as d]
   [integrant.core :as ig]
   [parts.state :as ps]))

(defrecord RefChat [db outbox])

(defn update-db [this fn & args]
  (apply update this :db commute fn args))

(defn update-outbox [this tx-fn & args]
  (apply update this :outbox commute tx-fn args))

(extend-type RefChat
  chat-api/IChatEvents
  (create-message! [this m]
    (update-db this datascript-commands/create-message m))
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
  (occupied-rooms [{:keys [db]}]
    (datascript-queries/occupied-rooms @db))
  (all-active-users [{:keys [db]}]
    (datascript-queries/all-active-users @db))
  (users-in-room [{:keys [db]} room-name]
    (datascript-queries/users-in-room @db room-name))
  (current-room-name [{:keys [db]} username]
    (datascript-queries/current-room-name @db username))
  (room [{:keys [db]} room-name]
    (datascript-queries/room @db room-name))
  (chat-history [{:keys [db]} room-name]
    (datascript-queries/chat-history @db room-name))
  reducer-api/IOutbox
  (outbox-write! [this evt]
    (update-outbox this conj evt))
  (outbox-read [{:keys [outbox]}] @outbox)
  (outbox-get [{:keys [outbox]} {to-find :uuid}]
    (first (filter (fn [{:keys [uuid]}] (= to-find uuid)) @outbox)))
  (outbox-delete! [this {to-be-removed :uuid}]
    (update
     this :outbox commute
     (fn [events] (vec (remove (fn [{:keys [uuid]}] (= uuid to-be-removed)) events))))
    (update-outbox this identity))
  reducer-api/IReducer
  (plan-and-execute! [this command]
    (dosync
     (let [planned-events (reducer-api/generate-plan this command)]
       (log/debugf
        "\nPLAN:\n%s" (with-out-str (pp/pprint planned-events)))
       (reducer-api/execute-events! this planned-events)))))

(defmethod ig/init-key ::ref-chat [_ initial-value]
  (log/debug "Creating ref-chat")
  (map->RefChat initial-value))

(def config
  {[::db ::ps/ref]     (d/empty-db datascript-schema/chat-schema)
   [::outbox ::ps/ref] []
   ::ref-chat          {:db     (ig/ref [::db ::ps/ref])
                        :outbox (ig/ref [::outbox ::ps/ref])}})
