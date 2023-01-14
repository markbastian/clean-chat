(ns clean-chat.stage04-isolate-events.system
  (:require [clean-chat.stage04-isolate-events.client-api :as client-api]
            [clean-chat.stage04-isolate-events.ws-handlers :as ws-handlers]
            [clean-chat.system :as system]
            [clean-chat.web :as web]
            [clojure.tools.logging :as log]
            [datascript.core :as d]
            [integrant.core :as ig]
            [parts.datascript.core.core :as ds]
            [parts.ring.adapter.jetty9.core :as jetty9]
            [parts.ws-handler :as ws]))

(def chat-schema
  {:username  {:db/unique :db.unique/identity}
   :room-name {:db/unique :db.unique/identity}
   :user      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}
   :room      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}})

(def client-schema
  {:client-id {:db/unique :db.unique/identity}})

(defmethod ig/init-key ::atom [_ initial-value]
  (log/debug "Creating atom")
  (atom initial-value))

(def config
  {[::chat-state ::ds/conn] {:schema chat-schema}
   [::clients-state ::atom] {}
   ::ws/ws-handlers         {:on-connect #'ws-handlers/on-connect
                             :on-text    #'ws-handlers/on-text
                             :on-close   #'ws-handlers/on-close
                             :on-error   #'ws-handlers/on-error}
   ::jetty9/server          {:title            "Welcome to Isolated Events Chat!"
                             :host             "0.0.0.0"
                             :port             3000
                             :join?            false
                             :clients          (ig/ref [::clients-state ::atom])
                             :conn             (ig/ref [::chat-state ::ds/conn])
                             :ws-handlers      (ig/ref ::ws/ws-handlers)
                             :ws-max-idle-time (* 10 60 1000)
                             :handler          #'web/handler}})

(comment
  (system/start config)
  (system/stop)
  (system/restart config)

  (let [conn (get (system/system) ::ds/conn)]
    @conn)

  (require '[clean-chat.stage04-isolate-events.domain :as domain])
  (require '[clean-chat.stage04-isolate-events.queries :as queries])
  (require '[clean-chat.stage04-isolate-events.commands :as commands])
  (require '[clean-chat.stage04-isolate-events.client-api :as client-api])
  (require '[datascript.core :as d])

  (let [conn (::ds/conn (system/system))
        id (:db/id (d/entity @conn [:room-name "foo"]))]
    (d/db-with @conn [[:db/add id :room-name "bar"]]))

  (let [conn (get (system/system) [::clients-state ::atom])]
    conn)

  (let [conn (get (system/system) [::chat-state ::ds/conn])]
    (commands/dispatch-command
      {:conn conn}
      {:command   :change-room
       :username  "Mark"
       :room-name "froob"}))

  (let [conn (::ds/conn (system/system))]
    (commands/dispatch-command
      {:conn conn}
      {:command      :chat-message
       :username     "Mark"
       :chat-message "What's going on?"}))

  (let [conn (::ds/conn (system/system))]
    (commands/dispatch-command
      {:conn conn}
      {:command   :change-room
       :username  "Mark"
       :room-name "public"}))

  (let [conn (::ds/conn (system/system))]
    (queries/chat-history @conn "public"))


  (let [conn (::ds/conn (system/system))]
    (d/q domain/all-active-users-query @conn))
  )
