(ns clean-chat.stage02-cqrs.system
  (:require [clean-chat.stage02-cqrs.ws-handlers :as ws-handlers]
            [clean-chat.system :as system]
            [clean-chat.web :as web]
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

(def config
  {::ds/conn        {:schema chat-schema}
   ::ws/ws-handlers {:on-connect #'ws-handlers/on-connect
                     :on-text    #'ws-handlers/on-text
                     :on-close   #'ws-handlers/on-close
                     :on-error   #'ws-handlers/on-error}
   ::jetty9/server  {:title            "Welcome to CQRS Chat!"
                     :host             "0.0.0.0"
                     :port             3000
                     :join?            false
                     :conn             (ig/ref ::ds/conn)
                     :ws-handlers      (ig/ref ::ws/ws-handlers)
                     :ws-max-idle-time (* 10 60 1000)
                     :handler          #'web/handler}})

(comment
  (system/start config)
  (system/stop)
  (system/restart config)

  (let [conn (get (system/system) ::ds/conn)]
    @conn)

  (require '[clean-chat.stage02-cqrs.domain :as domain])
  (require '[clean-chat.stage02-cqrs.queries :as queries])
  (require '[clean-chat.stage02-cqrs.commands :as commands])
  (require '[datascript.core :as d])

  (let [conn (::ds/conn (system/system))
        id (:db/id (d/entity @conn [:room-name "foo"]))]
    (d/db-with @conn [[:db/add id :room-name "bar"]]))

  (let [conn (::ds/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command   :change-room
      :username  "Mark"
      :room-name "froob"}))

  (let [conn (::ds/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command   :chat-message
      :username  "Mark"
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
    (d/q domain/all-active-users-query @conn)))
