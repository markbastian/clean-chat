(ns clean-chat.stage02-cqrs.rcb
  #_:clj-kondo/ignore
  (:require
   [clean-chat.stage02-cqrs.commands :as commands]
   [clean-chat.stage02-cqrs.queries :as queries]
   [clean-chat.system :as system]))

(comment
  (let [conn (:parts.datascript.core.core/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command   :change-room
      :username  "Mark"
      :room-name "froob"}))

  (let [conn (:parts.datascript.core.core/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command      :chat-message
      :username     "Mark"
      :chat-message "What's going on?"}))

  (let [conn (:parts.datascript.core.core/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command   :rename-room
      :username  "Mark"
      :room-name "bazwonk"}))

  (let [conn (:parts.datascript.core.core/conn (system/system))]
    (commands/dispatch-command
     {:conn conn}
     {:command   :change-room
      :username  "Mark"
      :room-name "public"}))

  (let [conn (:parts.datascript.core.core/conn (system/system))]
    (queries/chat-history @conn "public")))

;; I still got some issues
;; - I really can't test this - I just have automation and separation
;; - I need to separate out the client from the username
;; - I need to separate events from notifications
;; - Notifications should be generalizable
