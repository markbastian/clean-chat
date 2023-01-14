(ns clean-chat.stage06-sql.system
  (:require
   [clean-chat.stage06-sql.chat-api :as chat-api]
   [clean-chat.stage06-sql.chat-impl-atom :as cid]
   [clean-chat.stage06-sql.chat-impl-ref :as cir]
   [clean-chat.stage06-sql.chat-impl-sqlite :as cis]
   [clean-chat.stage06-sql.planex-api :as planex-api]
   [clean-chat.stage06-sql.queries-datascript :as queries]
   [clean-chat.stage06-sql.sql-migrations :as sql-migrations]
   [clean-chat.stage06-sql.ws-handlers :as ws-handlers]
   [clean-chat.system :as system]
   [clean-chat.web :as web]
   [clojure.tools.logging :as log]
   [datascript.core :as d]
   [integrant.core :as ig]
   [parts.next.jdbc.core :as jdbc]
   [parts.ring.adapter.jetty9.core :as jetty9]
   [parts.ws-handler :as ws]))

(defmethod ig/init-key ::atom [_ initial-value]
  (log/debug "Creating atom")
  (atom initial-value))

(defmethod ig/init-key ::ref [_ initial-value]
  (log/debug "Creating ref")
  (ref initial-value))

(defmethod ig/init-key ::ref-chat [_ initial-value]
  (log/debug "Creating ref-chat")
  ;; TODO - FIX - This won't work. This isn't a ref
  ;; Either the RefChat needs to be wrapped in a ref,
  ;; the values in the initial value need to be refs,
  ;; or this needs  to be 2 things (a chat ref and
  ;; an outbox ref).
  (cir/map->RefChat initial-value))

(defmethod ig/init-key ::sql-chat [_ m]
  (log/debug "Creating SQL Chat")
  (cis/map->SqlChat m))

(def config
  {[::chat-state ::atom]    (cid/map->DatascriptChat
                             {:db     (d/empty-db queries/chat-schema)
                              :outbox []})
   [::db ::ref]             (d/empty-db queries/chat-schema)
   [::outbox ::ref]         []
   ::ref-chat               {:db     (ig/ref [::db ::ref])
                             :outbox (ig/ref [::outbox ::ref])}
   [::clients-state ::atom] {}
   ::jdbc/datasource        {:dbtype       "sqlite"
                             :dbname       "chat-state"
                             :foreign_keys "on"}
   ::jdbc/migrations        {:db         (ig/ref ::jdbc/datasource)
                             :migrations [sql-migrations/create-room-table-sql
                                          sql-migrations/create-user-table-sql
                                          sql-migrations/create-message-table-sql
                                          sql-migrations/create-outbox-table-sql]}
   ::jdbc/teardown          {:db       (ig/ref ::jdbc/datasource)
                             :commands [sql-migrations/drop-outbox-table-sql
                                        sql-migrations/drop-message-table-sql
                                        sql-migrations/drop-user-table-sql
                                        sql-migrations/drop-room-table-sql]}
   ::sql-chat               {:db (ig/ref ::jdbc/datasource)}
   ::ws/ws-handlers         {:on-connect #'ws-handlers/on-connect
                             :on-text    #'ws-handlers/on-text
                             :on-close   #'ws-handlers/on-close
                             :on-error   #'ws-handlers/on-error}
   ::jetty9/server          {:title            "Welcome to Generalized API Chat!"
                             :host             "0.0.0.0"
                             :port             3000
                             :join?            false
                             :client-manager   (ig/ref [::clients-state ::atom])
                             :conn             (ig/ref ::sql-chat)
                             ;:conn             (ig/ref ::ref-chat)
                             ;:conn             (ig/ref [::chat-state ::atom])
                             :sqldb            (ig/ref ::jdbc/datasource)
                             :ws-handlers      (ig/ref ::ws/ws-handlers)
                             :ws-max-idle-time (* 10 60 1000)
                             :handler          #'web/handler}})

(comment
  (system/start config)
  (system/stop)
  (system/restart config)
  (system/system)

  (let [r (get (system/system) ::ref-chat)]
    (planex-api/generate-plan r {:command   :join-chat
                                 :username  "Mark"
                                 :room-name "public"}))

  (let [r (get (system/system) ::ref-chat)]
    (dosync
     (chat-api/join-chat! r {:username "Bob"})
     (chat-api/enter-room! r {:username "Bob"
                              :room-name "public"})
     (chat-api/create-message! r {:username "Bob"
                                  :room-name "public"
                                  :message   "Hi"}))))
