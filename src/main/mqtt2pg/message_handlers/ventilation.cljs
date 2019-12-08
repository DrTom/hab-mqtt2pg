(ns mqtt2pg.message-handlers.ventilation
  (:require
    [clojure.string :as s]
    [mqtt2pg.config :as config]
    [mqtt2pg.pg :as pg]
    [mqtt2pg.utils :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))

(def nevents* (atom {}))

(defn persist-averaged-nevents []
  (let [[events _] (reset-vals! nevents* {})]
    (doseq [[topic nvalues] events]
      (let [avr (/ (apply + nvalues) (count nvalues))
            value (condp #(s/ends-with? %2 %1) topic
                    "temperature" (/ (.round js/Math (* 10 avr)) 10)
                    (.round js/Math avr))]
        (debug 'persisting topic value nvalues)
        ; TODO use connection to persist not just the pool itself
        (.query @pg/pool* (clj->js 
                            {:name "number_events"
                             :text (str "INSERT INTO number_events (topic, value) "
                                        "VALUES ($1, $2)")
                             :values [topic, value]}) 
                (fn [err, res] 
                  (when err (error err))))))))


(defn on-message [topic message]
  (let [parsed-message (->> message (.parse js/JSON) js->clj clojure.walk/keywordize-keys)
        timestamp (or (:timestamp  parsed-message)) ; TODO or now
        value (or (:value parsed-message) message)
        nvalue (when (= js/Number  (some-> value type)) value)
        tvalue (when (string? value) value)]

    (debug 'on-message {:topic topic :parsed-message parsed-message :message message})

    ; for now we only care about floating point events

    (when nvalue ;;; number (floating point) event
      (swap! nevents* update-in [topic] #(conj %1 nvalue))
      (debug 'nevents* @nevents*)
      )))


(defn init []
  (js/setInterval persist-averaged-nevents (* 60 1000)))
