(ns clean-chat.stage06-sql.chat-impl-atom
  (:require [clean-chat.stage06-sql.chat-api :as chat-api]
            [clean-chat.stage06-sql.planex-api :as planex-api]
            [clean-chat.stage06-sql.queries-datascript :as queries]
            [clojure.tools.logging :as log]
            [datascript.core :as d]
            [integrant.core :as ig]))

(defrecord DatascriptChat [ctx])

(defn update-db [{:keys [ctx]} tx-data]
  (swap! ctx update :db d/db-with tx-data))

(defn update-outbox [{:keys [ctx]} fn & args]
  (apply swap! ctx update :outbox fn args))

(extend-type DatascriptChat
  chat-api/IChatEvents
  (create-message! [this {:keys [username message room-name]}]
    (update-db
     this
     [{:message message
       :user    {:username username}
       :room    {:room-name room-name}}]))
  (join-chat! [this {:keys [username]}]
    (update-db this [{:username username}]))
  (leave-chat! [this {:keys [username]}]
    (update-db this [[:db.fn/retractAttribute [:username username] :room]]))
  (create-room! [this {:keys [room-name]}]
    (update-db this [{:room-name room-name}]))
  (enter-room! [this {:keys [username room-name]}]
    (update-db this [{:username username :room {:room-name room-name}}]))
  (leave-room! [this {:keys [username]}]
    (update-db this [[:db.fn/retractAttribute [:username username] :room]]))
  (rename-room! [{:keys [ctx] :as this} {:keys [old-room-name new-room-name]}]
    (let [db      (:db @ctx)
          id      (:db/id (queries/room db old-room-name))
          tx-data [[:db/add id :room-name new-room-name]]]
      (update-db this tx-data)))
  chat-api/IChatQueries
  (occupied-rooms [{:keys [ctx]}]
    (queries/occupied-rooms (:db @ctx)))
  (all-active-users [{:keys [ctx]}]
    (queries/all-active-users (:db @ctx)))
  (users-in-room [{:keys [ctx]} room-name]
    (queries/users-in-room (:db @ctx) room-name))
  (current-room-name [{:keys [ctx]} username]
    (some-> (:db @ctx) (d/entity [:username username]) :room :room-name))
  (room [{:keys [ctx]} room-name]
    (d/entity (:db @ctx) [:room-name room-name]))
  ;; TODO NOTE THIS DOES NOT SEEM TO BE WORKING
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
     #(vec (remove (fn [{:keys [uuid]}] (= to-be-removed uuid)) %)))))

(defmethod ig/init-key ::atom-chat [_ initial-value]
  (log/debug "Creating atom-chat")
  (->DatascriptChat (atom initial-value)))

(def config
  {::atom-chat {:db     (d/empty-db queries/chat-schema)
                :outbox []}})

(update-db
 (->DatascriptChat
  (atom
   {:db     (d/empty-db queries/chat-schema)
    :outbox []}))
 [{:abc 123}])
