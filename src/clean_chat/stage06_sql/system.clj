(ns clean-chat.stage06-sql.system
  (:require
   [clean-chat.stage06-sql.chat-impl-atom :as cid]
   [clean-chat.stage06-sql.chat-impl-ref :as cir]
   [clean-chat.stage06-sql.chat-impl-sqlite :as cis]
   [clean-chat.stage06-sql.ws-handlers :as ws-handlers]
   [clean-chat.web :as web]
   [integrant.core :as ig]
   [parts.ring.adapter.jetty9.core :as jetty9]
   [parts.state :as ps]
   [parts.ws-handler :as ws]))

(def config
  (merge
   {[::clients-state ::ps/atom] {}
    ::ws/ws-handlers            {:on-connect #'ws-handlers/on-connect
                                 :on-text    #'ws-handlers/on-text
                                 :on-close   #'ws-handlers/on-close
                                 :on-error   #'ws-handlers/on-error}
    ::jetty9/server             {:title            "Welcome to Generalized API Chat!"
                                 :host             "0.0.0.0"
                                 :port             3000
                                 :join?            false
                                 :client-manager   (ig/ref [::clients-state ::ps/atom])
                                 :conn             (ig/ref ::cid/atom-chat)
                                 ;:conn             (ig/ref ::cis/sql-chat)
                                 ;:conn             (ig/ref ::cir/ref-chat)
                                 :ws-handlers      (ig/ref ::ws/ws-handlers)
                                 :ws-max-idle-time (* 10 60 1000)
                                 :handler          #'web/handler}}
   cid/config
   cis/config
   cir/config))

(comment
  (require '[clean-chat.system :as system])
  (system/start config)
  (system/stop)
  (system/restart config)
  (system/system)

  (let [r (get (system/system) [::clients-state ::ps/atom])]
    r)

  (require '[clean-chat.stage06-sql.planex-api :as planex-api])
  (let [r (get (system/system) ::cid/atom-chat)]
    r)

  (require '[clean-chat.stage06-sql.planex-api :as planex-api])
  (let [r (get (system/system) ::cir/ref-chat)]
    r)

  (require '[clean-chat.stage06-sql.chat-api :as chat-api])
  (require '[clean-chat.stage06-sql.broker-ref :as broker-ref])
  (let [c (get (system/system) [::clients-state ::ps/atom])
        r (get (system/system) ::cir/ref-chat)]
    (dosync
     (broker-ref/process-command
      {:clients @c
       :conn    r}
      {:command   :join-chat
       :username  "Bob"
       :room-name "public"})))

  (let [c (get (system/system) [::clients-state ::ps/atom])
        r (get (system/system) ::cir/ref-chat)]
    (dosync
     (broker-ref/process-command
      {:clients @c
       :conn    r}
      {:command      :chat-message
       :username     "Bob"
       :chat-message "HEY!"})))

  (let [c (get (system/system) [::clients-state ::ps/atom])
        r (get (system/system) ::cir/ref-chat)]
    (dosync
     (broker-ref/process-command
      {:clients @c
       :conn    r}
      {:command  :leave-chat
       :username "Bob"}))))
