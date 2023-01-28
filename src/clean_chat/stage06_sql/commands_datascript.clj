(ns clean-chat.stage06-sql.commands-datascript
  (:require [clean-chat.stage06-sql.queries-datascript :as datascript-queries]
            [datascript.core :as d]))

(defn create-message [db {:keys [uuid username message room-name nanos] :as m}]
  {:pre [uuid username message room-name nanos]}
  (d/db-with db [(-> m
                     (select-keys [:nanos :uuid :message])
                     (assoc
                      :user {:username username}
                      :room {:room-name room-name}))]))

(defn join-chat [db {:keys [username]}]
  {:pre [username]}
  (d/db-with db [{:username username}]))

(defn insert-room [db room-name]
  (d/db-with db [{:room-name room-name}]))

(defn enter-room [db {:keys [username room-name]}]
  (d/db-with db [{:username username :room {:room-name room-name}}]))

(defn leave-room [db {:keys [username]}]
  {:pre [username]}
  (d/db-with db [[:db.fn/retractAttribute [:username username] :room]]))

(defn rename-room [db {:keys [old-room-name new-room-name]}]
  (let [id      (:db/id (datascript-queries/room db old-room-name))
        tx-data [[:db/add id :room-name new-room-name]]]
    (d/db-with db tx-data)))
