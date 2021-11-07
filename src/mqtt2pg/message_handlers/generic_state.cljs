(ns mqtt2pg.message-handlers.generic-state
  (:require
    [mqtt2pg.config :as config]
    [mqtt2pg.utils.core :refer [presence]]
    [mqtt2pg.db :as db]
    [clojure.string :as s]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))


(def last-persisted-values* (atom {}))

(defn parse [message]
  (try
    (->> message
         js/JSON.parse
         js->clj)
    (catch js/Object e
      (debug "failed to parse message " message e)
      message)))

(defn on-message [topic message]
  (debug 'on-message topic message)
  (let [value (parse message)
        table (cond
                (number? value) "number_events"
                (map? value) "data_events"
                (string? value) "text_events"
                :else nil)]
    (if-not table
      (error "Failed to assign " table " on " topic " for " message)
      (if (= value (get @last-persisted-values* topic))
        (debug "skip persisting same value" topic value)
        ; TODO use connection to persist not just the pool itself
        (.query @db/pool* (clj->js
                            {:name table
                             :text (str "INSERT INTO " table " (topic, value) "
                                        "VALUES ($1, $2)")
                             :values [topic, value]})
                (fn [err, res]
                  (if err
                    (error err)
                    (swap! last-persisted-values* assoc topic value))))))))



