(ns clean-chat.stage06-sql.client-impl-ws
  (:require
   ;[clean-chat.stage06-sql.chat-api :as chat-api]
   [clean-chat.stage06-sql.client-api :as client-api]
   [clojure.tools.logging :as log]
   [ring.adapter.jetty9 :as jetty]))

(defrecord WebSocketClient [client-id ws transform])

(extend-type WebSocketClient
  client-api/IClient
  (send! [{:keys [ws client-id]} message]
    (log/tracef "Sending %s to %s via ws." message client-id)
    (jetty/send! ws message))

  ; Not yet. Gotta think about aggregation
  ;chat-api/IChatNotifications
  ;(join-chat [this notification])
  ;(leave-chat [this notification])
  ;(create-message [this notification])
  ;(enter-room [this notification])
  ;(leave-room [this notification])
  ;(create-room [this notification])
  ;(rename-room [this notification])
  )
