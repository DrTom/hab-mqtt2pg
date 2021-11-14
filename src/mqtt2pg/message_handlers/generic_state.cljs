(ns mqtt2pg.message-handlers.generic-state
  (:require
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [clojure.string :as s]
    [mqtt2pg.config :as config]
    [mqtt2pg.db.insert :as db-insert :refer [insert*]]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))


(defn parse [message]
  (try
    (->> message
         js/JSON.parse
         js->clj)
    (catch js/Object e
      (debug "failed to parse message " message e)
      message)))

(defn on-message [topic message]
  (go (try
        (<? (insert* topic (parse message)))
        (catch js/Error e
          (warn (ex-message e) [topic message])))))



