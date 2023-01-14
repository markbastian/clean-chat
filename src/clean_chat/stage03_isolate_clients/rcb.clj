(ns clean-chat.stage03-isolate-clients.rcb
  (:require [clean-chat.stage03-isolate-clients.commands :as commands]
            [clean-chat.stage03-isolate-clients.htmx-notifications :as htmx-notifications]
            [clean-chat.stage03-isolate-clients.queries :as queries]
            [clean-chat.stage03-isolate-clients.system :as config]
            [clean-chat.system :as system]
            [parts.datascript.core.core :as ds]))

(def chat-state-key [::config/chat-state ::ds/conn])
(def client-state-key [::config/clients-state ::ds/conn])

(comment
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