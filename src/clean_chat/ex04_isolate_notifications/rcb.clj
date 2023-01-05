(ns clean-chat.ex04-isolate-notifications.rcb
  (:require
    [clean-chat.ex04-isolate-notifications.config :as config]
    [clean-chat.ex04-isolate-notifications.commands :as commands]
    [clean-chat.ex04-isolate-notifications.queries :as queries]
    [clean-chat.ex04-isolate-notifications.htmx-notifications :as htmx-notifications]
    [clean-chat.system :as system]
    [datascript.core :as d]
    [parts.datascript.core.core :as ds]))

(def chat-state-key [::config/chat-state ::ds/conn])
(def client-state-key [::config/clients-state ::ds/conn])

(comment
  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (d/q
      '[:find ?transform (pull ?e [*])
        :in $
        :where
        [?e :transform ?transform]]
      @clients))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
      {:conn conn :clients clients}
      {:command   :change-room
       :username  "Mark"
       :room-name "froob"}))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
      {:conn conn :clients clients}
      {:command      :chat-message
       :username     "Mark"
       :chat-message "What's going on?"}))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
      {:conn conn :clients clients}
      {:command   :rename-room
       :username  "Mark"
       :room-name "bazwonk"}))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
      {:conn conn :clients clients}
      {:command   :change-room
       :username  "Mark"
       :room-name "public"}))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (commands/dispatch-command
      {:conn conn :clients clients}
      {:command   :change-room
       :username  "Mark"
       :room-name "public"}))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (htmx-notifications/broadcast-to-room @clients @conn "public" "HEY"))

  (let [conn (get (system/system) chat-state-key)
        clients (get (system/system) client-state-key)]
    (queries/chat-history @conn "public")))

;; I still got some issues
;; - I really can't test this - I just have automation and separation
;; - I need to separate events from notifications
;; - Notifications should be generalizable