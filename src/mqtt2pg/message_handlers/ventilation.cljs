(ns mqtt2pg.message-handlers.ventilation
  (:require
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [clojure.string :as s]
    [mqtt2pg.config :as config]
    [mqtt2pg.db :as db]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))

(defonce nevents* (atom {}))

(defn persist-averaged-nevents []
  (try
    (debug 'persist-averaged-nevents)
    (let [[events _] (reset-vals! nevents* {})]
      (doseq [[topic nvalues] events]
        (let [avr (/ (apply + nvalues) (count nvalues))
              value (condp #(s/ends-with? %2 %1) topic
                      "temperature" (/ (.round js/Math (* 10 avr)) 10)
                      (.round js/Math avr))]
          (debug 'persisting topic value nvalues)
          (.query @db/pool* (clj->js
                              {:name "number_events"
                               :text (str "INSERT INTO number_events (topic, value) "
                                          "VALUES ($1, $2)")
                               :values [topic, value]})
                  (fn [err, res]
                    (when err (error err)))))))
    (catch js/Error e
      (error e))))


(defn on-message [topic message]
  (let [parsed-message (->> message (.parse js/JSON) js->clj clojure.walk/keywordize-keys)
        timestamp (or (:timestamp  parsed-message)) ; TODO or now
        value (or (:value parsed-message) message)
        nvalue (when (= js/Number(some-> value type)) value)
        tvalue (when (string? value) value)]
    (debug 'on-message {:topic topic :parsed-message parsed-message :message message})
    ; for now we only care about floating point events
    (when nvalue ;;; number (floating point) event
      (swap! nevents* update-in [topic] #(conj %1 nvalue))
      (debug 'nevents* @nevents*))))

(defonce loop-ts* (atom nil))

(defn init []
  (let [loop-ts (js/Date.)]
    (reset! loop-ts* loop-ts)
    (info "Starting persist-averaged-nevents loop " loop-ts)
    (go (while (= loop-ts @loop-ts*)
          (persist-averaged-nevents)
          (<! (timeout (* 60 1000)))))))
