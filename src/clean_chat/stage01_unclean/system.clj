(ns clean-chat.stage01-unclean.system
  (:require [clean-chat.stage01-unclean.ws-handlers :as ws-handlers]
            [clean-chat.web :as web]
            [integrant.core :as ig]
            [parts.datascript.core.core :as ds]
            [parts.ring.adapter.jetty9.core :as jetty9]
            [parts.ws-handler :as ws]))

(def chat-schema
  {:username  {:db/unique :db.unique/identity}
   :ws        {:db/unique :db.unique/identity}
   :room-name {:db/unique :db.unique/identity}
   :room      {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}})

(def config
  {::ds/conn        {:schema chat-schema}
   ::ws/ws-handlers {:on-connect #'ws-handlers/on-connect
                     :on-text    #'ws-handlers/on-text
                     :on-close   #'ws-handlers/on-close
                     :on-error   #'ws-handlers/on-error}
   ::jetty9/server  {:title            "Welcome to Cowboy Chat!"
                     :host             "0.0.0.0"
                     :port             3000
                     :join?            false
                     :conn             (ig/ref ::ds/conn)
                     :ws-handlers      (ig/ref ::ws/ws-handlers)
                     :ws-max-idle-time (* 10 60 1000)
                     :handler          #'web/handler}})

(comment
  (require '[clean-chat.system :as system])
  (system/start config)
  (system/stop)
  (system/restart config)

  (let [conn (::ds/conn (system/system))]
    @conn)

  (require '[clean-chat.stage01-unclean.domain :as domain])
  (require '[datascript.core :as d])
  (let [conn (::ds/conn (system/system))]
    (d/q domain/all-active-users-query @conn)))
