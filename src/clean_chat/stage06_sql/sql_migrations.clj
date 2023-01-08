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
      [:room-uuid :uuid]
      [[:foreign-key :room-uuid] [:references :room :uuid]]]}))

(def create-message-table-sql
  (hsql/format
    {:create-table [:message :if-not-exists]
     :with-columns
     [[:uuid :uuid :primary-key [:not nil]]
      [:user-uuid :uuid [:not nil]]
      [:room-uuid :uuid [:not nil]]
      [:message :varchar [:not nil]]
      [:nanos :long [:not nil]]
      [[:foreign-key :user-uuid] [:references :user :uuid]]
      [[:foreign-key :room-uuid] [:references :room :uuid]]]}))

(def create-outbox-table-sql
  (hsql/format
    {:create-table [:outbox :if-not-exists]
     :with-columns
     [[:uuid :uuid [:primary-key] [:not nil]]
      [:nanos :long [:not nil]]
      [:event :text]]}))

(def drop-message-table-sql
  (hsql/format '{drop-table (if-exists message)}))

(def drop-user-table-sql
  (hsql/format '{drop-table (if-exists user)}))

(def drop-room-table-sql
  (hsql/format '{drop-table (if-exists room)}))

(def drop-outbox-table-sql
  (hsql/format '{drop-table (if-exists outbox)}))
