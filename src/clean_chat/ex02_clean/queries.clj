(ns clean-chat.ex02-clean.queries
  (:require [datascript.core :as d]))

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

(def all-ws-query
  '[:find [?ws ...] :in $ :where [?e :ws ?ws]])

(defn all-clients [db]
  (d/q all-ws-query db))

(defn clients-in-room [db room-name]
  (d/q
    '[:find [?ws ...]
      :in $ ?room-name
      :where
      [?room :room-name ?room-name]
      [?client :room ?room]
      [?client :ws ?ws]]
    db room-name))

(def username->ws+room-query
  '[:find ?ws ?room-name
    :keys ws room-name
    :in $ ?username
    :where
    [?e :username ?username]
    [?e :ws ?ws]
    [?e :room ?r]
    [?r :room-name ?room-name]])

(defn ws+room-name [db username]
  (first
    (d/q
      username->ws+room-query
      db username)))

(defn current-room-name [db username]
  (some-> db (d/entity [:username username]) :room :room-name))