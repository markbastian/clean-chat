(ns clean-chat.web
  (:require [clean-chat.pages :as chat-pages]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty9 :as jetty]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.http-response :refer [internal-server-error not-found ok]]))

(defn ws-upgrade-handler [{:keys [ws-handlers] :as context} upgrade-request]
  (let [{:keys [on-connect on-text on-bytes on-close on-ping on-pong on-error]} ws-handlers
        provided-subprotocols (:websocket-subprotocols upgrade-request)
        provided-extensions (:websocket-extensions upgrade-request)]
    {:on-connect  (partial on-connect context)
     :on-text     (partial on-text context)
     :on-bytes    (partial on-bytes context)
     :on-close    (partial on-close context)
     :on-ping     (partial on-ping context)
     :on-pong     (partial on-pong context)
     :on-error    (partial on-error context)
     :subprotocol (first provided-subprotocols)
     :extensions  provided-extensions}))

(defn ws-handler
  ([request]
   (if (jetty/ws-upgrade-request? request)
     (jetty/ws-upgrade-response (partial ws-upgrade-handler request))
     (internal-server-error "Cannot upgrade request")))
  ([request resp _raise]
   (resp (ws-handler request))))

(defn landing-page-handler [{:keys [title]}]
  (ok (chat-pages/landing-page title)))

(defn get-chatroom-page-handler [{:keys [params] :as request}]
  (let [{:keys [username room-name]
         :or   {username "TESTUSER" room-name "TESTROOM"}} params
        args {:username username :room-name room-name}]
    (ok (chat-pages/wrap-as-page
         (chat-pages/chat-page
          (update request :params merge args))))))

(defn post-chatroom-page-handler [request]
  (ok (chat-pages/chat-page request)))

(def routes
  [["/chat" {:handler landing-page-handler}]
   ["/chat/ws/:room-name/:username" {:handler    ws-handler
                                     :parameters {:path {:room-name string?
                                                         :username  string?}}}]
   ["/chat/room" {:get  get-chatroom-page-handler
                  :post post-chatroom-page-handler}]])

(def handler
  (ring/ring-handler
   (ring/router
    routes
    {:data {:middleware [[wrap-defaults
                          (-> site-defaults
                              (update :security dissoc :anti-forgery)
                              (update :security dissoc :content-type-options)
                              (update :responses dissoc :content-types))]
                           ;wrap-params
                         wrap-json-response
                         parameters/parameters-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware]}})
   (constantly (not-found "Not found"))))
