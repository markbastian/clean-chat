(ns clean-chat.stage03-isolate-clients.queries
  (:require
   [datascript.core :as d]))

(def all-rooms-query
  '[:find [?room-name ...]
    :in $
    :where
    [_ :room-name ?room-name]])

(def all-active-users-query
  '[:find ?username ?room-name
    :keys username room-name
    :in $
    :where
    [?e :username ?username]
    [?e :room ?r]
    [?r :room-name ?room-name]])

(defn occupied-rooms [db]
  (sort (d/q all-rooms-query db)))

(defn all-active-users [db]
  (->> (d/q all-active-users-query db) (map :username) sort))

(defn users-in-room [db room-name]
  (d/q
   '[:find [?username ...]
     :in $ ?room-name
     :where
     [?room :room-name ?room-name]
     [?client :room ?room]
     [?client :username ?username]]
   db room-name))

(defn current-room-name [db username]
  (some-> db (d/entity [:username username]) :room :room-name))

(defn room [db room-name]
  (d/entity db [:room-name room-name]))

(defn room-exists? [db room-name]
  (some? (d/entity db [:room-name room-name])))

(defn chat-history [db room-name]
  (->> (d/q
        '[:find ?username ?message ?t
          :keys username message t
          :in $ ?room-name
          :where
          [?r :room-name ?room-name]
          [?m :room ?r]
          [?m :user ?u]
          [?u :username ?username]
          [?m :message ?message]
          [?m :nanos-since-unix-epoch ?t]]
        db room-name)
       (sort-by :t)))
