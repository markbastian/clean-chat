(ns clean-chat.stage06-sql.rcb
  (:require [clean-chat.system :as system]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clean-chat.stage06-sql.sql-queries :as sql-queries]))

(comment
  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT 1"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT * FROM room"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT * FROM user"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT * FROM message"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT * FROM outbox"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (sql/insert! ds :room {:name "public"}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (sql/update! ds :user {:name "Mark" :room_name "public"} ["name = ?" "Mark"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (sql/insert! ds :message
                 {:user_name "Mark"
                  :nanos     (System/nanoTime)
                  :room_name "public"
                  :message   "Hi!"}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    [(sql-queries/upsert-room! ds {:name "public"})
     (sql-queries/upsert-room! ds {:name "FOO"})
     (sql-queries/upsert-room! ds {:name "private"})
     (sql-queries/upsert-user! ds {:name "Sam"})
     (sql-queries/upsert-user! ds {:name "Mark"})
     (sql-queries/upsert-user! ds {:name "Joe"})
     (sql-queries/upsert-user! ds {:name "Bob" :room_name "FOO"})
     (sql-queries/upsert-user! ds {:username "Bob" :room-name "FOO"})
     (sql-queries/upsert-user! ds {:username "Mark" :room-name "public"})
     (sql-queries/upsert-user! ds {:username "Sam" :room-name "public"})
     (sql-queries/upsert-user! ds {:username "Joe" :room-name nil})
     ;(sql-queries/insert-room! ds {:name (str (gensym "ROOM"))})
     ;(sql-queries/insert-message! ds {:username "Mark" :room-name "FOO" :message "Hi!!"})
     ;(sql-queries/insert-message! ds {:username "Bob" :room-name "FOO" :message "Hi!!"})
     ;(sql-queries/get-messages ds)
     ]
    (sql-queries/occupied-rooms ds)
    (sql-queries/all-active-users ds)
    (sql-queries/users-in-room ds "public")
    (sql-queries/current-room-name ds "Sam")
    (sql-queries/get-room ds {:name "public"})
    (sql-queries/insert-message! ds {:username "Mark" :room-name "public" :message "Hi!!"})
    (sql-queries/get-messages-for-room ds "public"))

  ;; Create users, rooms, messages, and outbox tables
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/clause-reference.md
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/special-syntax.md#column-descriptors

  )


