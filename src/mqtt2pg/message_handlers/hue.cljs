(ns mqtt2pg.message-handlers.hue
  (:require
    [clojure.walk]
    [mqtt2pg.config :as config]
    [mqtt2pg.utils.core :refer [presence]]
    [mqtt2pg.db :as db]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))


(def last-persisted-values* (atom {}))

(defn parse [message]
  (try
    (-> message js/JSON.parse js->clj clojure.walk/keywordize-keys)
    (catch js/Object e
      (debug "failed to parse message " message e)
      message)))

(defn persist [table topic value timestamp]
  (when (not= (get @last-persisted-values* topic) value)
    (.query @db/pool* (clj->js
                        {:name table
                         :text (str "INSERT INTO " table " (topic, value, time) "
                                    "VALUES ($1, $2, $3)")
                         :values [topic, value, timestamp]})
            (fn [err, res]
              (if err
                (error err)
                (swap! last-persisted-values* assoc topic value))))))

(defn number-event [topic message]
  (let [value (case (:type message)
                "illumination" (:lightlevel message)
                "temperature" (:temperature message))]
    (persist
      "number_events"
      topic
      value
      (js/Date. (:timestamp message)))))

(defn presence-event [topic message]
  (persist
    "data_events"
    topic
    (:presence message)
    (js/Date. (:timestamp message))))

(defn on-message [topic message]
  (debug 'on-message topic message)
  (let [message (parse message)]
    (case (:type message)
      ("illumination" "temperature") (number-event topic message)
      "presence" (presence-event topic message))))

