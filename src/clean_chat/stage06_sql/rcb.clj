(ns clean-chat.stage06-sql.rcb
  #_:clj-kondo/ignore
  (:require [clean-chat.stage06-sql.chat-impl-sqlite]
            [clean-chat.stage06-sql.domain-sql :as domain-sql]
            [clean-chat.system :as system]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

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

  (let [chat (:clean-chat.stage06-sql.system/sql-chat (system/system))]
    (reducer-api/execute-plan! chat {:event :join-chat :username "A"}))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    [(domain-sql/upsert-room! ds {:name "public"})
     (domain-sql/upsert-room! ds {:name "FOO"})
     (domain-sql/insert-room! ds {:name (str (gensym "FOO"))})
     (domain-sql/upsert-room! ds {:name "private"})
     (domain-sql/get-room ds {:name "private"})
     (domain-sql/upsert-user! ds {:name "Sam"})
     (domain-sql/get-user ds {:name "Sam"})
     (domain-sql/upsert-user! ds {:name "Mark"})
     (domain-sql/insert-user! ds {:name (str (gensym "Bob"))})
     (domain-sql/insert-user! ds {:name       (str (gensym "Bob"))
                                  :room-name "FOO"})
     (domain-sql/update-user! ds {:name       "Mark"
                                  :room-name "private"})
     (domain-sql/update-user! ds {:name       "Mark"
                                  :room-name nil})
     (domain-sql/update-user! ds {:name       "Mark"
                                  :room-name "public"})
     (domain-sql/upsert-user! ds {:name       "Sam"
                                  :room-name "FOO"})
     (domain-sql/upsert-user! ds {:name "Bob" :room_name "FOO"})
     (domain-sql/upsert-user! ds {:username "Bob" :room-name "FOO"})
     (domain-sql/upsert-user! ds {:username "Mark" :room-name "public"})
     (domain-sql/upsert-user! ds {:username "Sam" :room-name "public"})
     (domain-sql/upsert-user! ds {:username "Joe" :room-name nil})
     (domain-sql/insert-room! ds {:name (str (gensym "ROOM"))})
     (domain-sql/insert-message! ds {:username "Mark" :room-name "FOO" :message "Hi!!"})
     (domain-sql/insert-message! ds {:username "Bob" :room-name "FOO" :message "Hi!!"})
     (domain-sql/get-messages ds)
     (domain-sql/occupied-rooms ds)
     (domain-sql/all-active-users ds)
     (domain-sql/users-in-room ds "public")
     (domain-sql/current-room-name ds "Sam")
     (domain-sql/get-room ds {:name "public"})
     (domain-sql/insert-message! ds {:username "Mark" :room-name "public" :message "Hi!!"})
     (domain-sql/get-messages-for-room ds "public")
     (domain-sql/insert-outbox-event! ds {:event :join-room :username "Mark"})
     (domain-sql/get-outbox-events ds)])

  (let [ds (:clean-chat.stage06-sql.system/sql-chat (system/system))]
    (reducer-api/outbox-read ds))

  ;; Create users, rooms, messages, and outbox tables
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/clause-reference.md
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/special-syntax.md#column-descriptors
  )
