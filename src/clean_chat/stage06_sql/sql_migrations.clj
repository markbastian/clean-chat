(ns clean-chat.stage06-sql.sql-migrations
  (:require [honey.sql :as hsql]))

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
