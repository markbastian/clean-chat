(ns parts.ring.adapter.jetty9.core
  (:require [clean-chat.middleware :refer [wrap-component]]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.adapter.jetty9 :as jetty9])
  (:import [org.eclipse.jetty.server Server]))

(defmethod ig/init-key ::server [_ {:keys [handler] :as m}]
  (log/debug "Launching Jetty web server.")
  (jetty9/run-jetty (wrap-component handler m) m))

(defmethod ig/halt-key! ::server [_ ^Server server]
  (log/debug "Stopping Jetty web server.")
  (.stop server))
