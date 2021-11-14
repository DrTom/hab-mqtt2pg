(ns mqtt2pg.message-handlers.hue
  (:require
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [clojure.walk]
    [mqtt2pg.config :as config]
    [mqtt2pg.db.insert :as db-insert]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))


(defn parse [message]
  (try (-> message js/JSON.parse js->clj
           clojure.walk/keywordize-keys)
       (catch js/Object e
         (warn "failed to parse message " message e)
         (throw e))))


(defn on-message [topic message]
  (debug 'on-message topic message)
  (go (try (let [message (parse message)
                  ts (some-> message :timestamp js/Date.)
                  dv  (case (:type message)
                        "illumination" (:lightlevel message)
                        "temperature" (:temperature message)
                        "presence" (:presence message))]
              (<? (db-insert/insert* topic dv :ts ts)))
           (catch js/Error e
             (warn (ex-message e) {:topic topic :message message})))))

