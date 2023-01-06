(ns clean-chat.stage06-sql.system
  (:require
    [clean-chat.web :as web]
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [integrant.core :as ig]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [parts.ws-handler :as ws]
    [parts.next.jdbc.core :as jdbc]
    [clean-chat.system :as system]
    [clean-chat.stage06-sql.ws-handlers :as ws-handlers]
    [clean-chat.stage06-sql.chat-impl-datascript :as cid]))

(def chat-schema
  {:username  {:db/unique :db.unique/identity}
   :room-name {:db/unique :db.unique/identity}
   :user      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}
   :room      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}})

(defmethod ig/init-key ::atom [_ initial-value]
  (log/debug "Creating atom")
  (atom initial-value))

(def config
  {[::chat-state ::atom]    (cid/map->DatascriptChat
                              {:db     (d/empty-db chat-schema)
                               :outbox []})
   [::clients-state ::atom] {}
   ::jdbc/datasource        {:dbtype "sqlite"
                             :dbname "chat-state"}
   ::ws/ws-handlers         {:on-connect #'ws-handlers/on-connect
                             :on-text    #'ws-handlers/on-text
                             :on-close   #'ws-handlers/on-close
                             :on-error   #'ws-handlers/on-error}
   ::jetty9/server          {:title            "Welcome to Generalized API Chat!"
                             :host             "0.0.0.0"
                             :port             3000
                             :join?            false
                             :clients          (ig/ref [::clients-state ::atom])
                             :conn             (ig/ref [::chat-state ::atom])
                             :sqldb            (ig/ref ::jdbc/datasource)
                             :ws-handlers      (ig/ref ::ws/ws-handlers)
                             :ws-max-idle-time (* 10 60 1000)
                             :handler          #'web/handler}})

(comment
  (system/start config)
  (system/stop)
  (system/restart config)
  (system/system)
  )
