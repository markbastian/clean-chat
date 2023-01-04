(ns clean-chat.ex02-cqrs.rcb
  (:require
    [clean-chat.ex02-cqrs.commands :as commands]
    [clean-chat.ex02-cqrs.queries :as queries]
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