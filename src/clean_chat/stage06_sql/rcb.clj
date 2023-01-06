(ns clean-chat.stage06-sql.rcb
  (:require [clean-chat.system :as system]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [honey.sql :as hsql]))

(def create-room-table-sql
  (hsql/format
    {:create-table [:room :if-not-exists]
     :with-columns
     [[:uuid :uuid :primary-key [:not nil]]
      [:name :varchar :unique [:not nil]]]}))

(def create-user-table-sql
  (hsql/format
    {:create-table [:user :if-not-exists]
     :with-columns
     [[:uuid :uuid :primary-key [:not nil]]
      [:name :varchar :unique [:not nil]]
      [:room-name :varchar]
      [[:foreign-key :room-name] [:references :room :name]]]}))

(def create-message-table-sql
  (hsql/format
    {:create-table [:message :if-not-exists]
     :with-columns
     [[:uuid :uuid :primary-key [:not nil]]
      [:user-name :varchar [:not nil]]
      [:room-name :varchar [:not nil]]
      [:message :varchar [:not nil]]
      [:nanos :long [:not nil]]
      [[:foreign-key :user-name] [:references :user :name]]
      [[:foreign-key :room-name] [:references :room :name]]]}))

(def create-outbox-table-sql
  (hsql/format
    {:create-table [:outbox :if-not-exists]
     :with-columns
     [[:uuid :uuid [:primary-key] [:not nil]]
      [:event :text]]}))

(def drop-message-table-sql
  (hsql/format '{drop-table (if-exists message)}))

(def drop-user-table-sql
  (hsql/format '{drop-table (if-exists user)}))

(def drop-room-table-sql
  (hsql/format '{drop-table (if-exists room)}))

(def drop-outbox-table-sql
  (hsql/format '{drop-table (if-exists outbox)}))

(comment
  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds ["SELECT 1"]))

  (let [ds (:parts.next.jdbc.core/datasource (system/system))]
    (jdbc/execute! ds create-room-table-sql)
    (jdbc/execute! ds create-user-table-sql)
    (jdbc/execute! ds create-message-table-sql)
    (jdbc/execute! ds create-outbox-table-sql))

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
    (jdbc/execute! ds drop-message-table-sql)
    (jdbc/execute! ds drop-user-table-sql)
    (jdbc/execute! ds drop-room-table-sql)
    (jdbc/execute! ds drop-outbox-table-sql))

  ;; Create users, rooms, messages, and outbox tables
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/clause-reference.md
  ;; https://github.com/seancorfield/honeysql/blob/develop/doc/special-syntax.md#column-descriptors

  )


