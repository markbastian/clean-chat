(ns parts.next.jdbc.core
  (:require
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [next.jdbc :as jdbc]))

(defmethod ig/init-key ::datasource [_ opts]
  (log/debug "Getting jdbc datasource.")
  (jdbc/get-datasource opts))