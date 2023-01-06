(ns clean-chat.stage05-planbox.system
  (:require
    [clean-chat.web :as web]
    [clojure.tools.logging :as log]
    [datascript.core :as d]
    [integrant.core :as ig]
    [parts.datascript.core.core :as ds]
    [parts.ring.adapter.jetty9.core :as jetty9]
    [parts.ws-handler :as ws]
    [clean-chat.system :as system]
    [clean-chat.stage05-planbox.ws-handlers :as ws-handlers]))

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
  {[::chat-state ::atom]    {:db     (d/empty-db chat-schema)
                             :outbox []}
   [::clients-state ::atom] {}
   ::ws/ws-handlers         {:on-connect #'ws-handlers/on-connect
                             :on-text    #'ws-handlers/on-text
                             :on-close   #'ws-handlers/on-close
                             :on-error   #'ws-handlers/on-error}
   ::jetty9/server          {:title            "Welcome to Planbox Chat!"
                             :host             "0.0.0.0"
                             :port             3000
                             :join?            false
                             :clients          (ig/ref [::clients-state ::atom])
                             :conn             (ig/ref [::chat-state ::atom])
                             :ws-handlers      (ig/ref ::ws/ws-handlers)
                             :ws-max-idle-time (* 10 60 1000)
                             :handler          #'web/handler}})

(comment
  (system/start config)
  (system/stop)
  (system/restart config)
  )
