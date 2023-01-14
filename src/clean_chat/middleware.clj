(ns clean-chat.middleware)

(defn wrap-component [handler component]
  (fn [request] (handler (into component request))))
