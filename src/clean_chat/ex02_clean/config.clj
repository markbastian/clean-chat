(ns clean-chat.ex02-clean.config
  (:require
    [clean-chat.ex02-clean.domain :as domain]
    [clean-chat.web :as web]
    [datascript.core :as d]
    [integrant.core :as ig]
    [parts.datascript.core.core :as ds]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [parts.ws-handler :as ws]
    [clean-chat.system :as system]
    [clean-chat.ex02-clean.ws-handlers :as ws-handlers]))

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
   ::jetty9/server  {:title            "Welcome to Atomic Clean Chat!"
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

  (let [conn (::ds/conn (system/system))]
    @conn)

  (require '[clean-chat.ex02-clean.domain :as domain])
  (require '[datascript.core :as d])
  (let [conn (::ds/conn (system/system))]
    (d/q domain/all-active-users-query @conn))

  )
