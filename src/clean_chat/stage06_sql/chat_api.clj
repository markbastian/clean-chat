(ns clean-chat.stage06-sql.chat-api)

(defprotocol IChatEvents
  (create-message [this event])
  (join-chat [this event])
  (leave-chat [this event])
  (create-room [this event])
  (enter-room [this event])
  (leave-room [this event])
  (rename-room [this event]))

(defprotocol IChatQueries
  (occupied-rooms [this])
  (all-active-users [this])
  (users-in-room [this room-name])
  (current-room-name [this username])
  (room [this room-name])
  (chat-history [this event]))