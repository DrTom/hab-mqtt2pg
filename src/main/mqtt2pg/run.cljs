(ns mqtt2pg.run
  (:require
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [clojure.walk]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.pg :as pg]
    [mqtt2pg.utils :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))

(def mqtt-client* (atom nil))
(def pg* (atom nil))
(def opts* (atom nil))


;;; message handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-message [topic message]
  (let [parsed-message (->> message .toString (.parse js/JSON) js->clj clojure.walk/keywordize-keys)
        timestamp (or (:timestamp  parsed-message)) ; TODO or now
        value (or (:value parsed-message) message)
        nvalue (when (= js/Number  (some-> value type)) value)
        tvalue (when (string? value) value)]


    (debug 'on-message {:topic topic :parsed-message parsed-message :message message})


    ; for now we only care about floating point events

    (when nvalue ;;; number (floating point) event
      (.query @pg* (clj->js 
                     {:name "number_events"
                      :text (str "INSERT INTO number_events (topic, time, value) "
                                 "VALUES ($1, $2, $3)")
                      :values [topic, timestamp, nvalue]}) 
              (fn [err, res] 
                (when err (error err)))))))

;;; exit handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exit []
  (when-let [pg @pg*]
    (.end pg))
  (when-let [mqtt-client @mqtt-client*]
    (.end mqtt-client))
  (js/setTimeout #(js/process.exit 0) 500))

(defn listen-on-exit-signals []
  (.on js/process "SIGINT" exit)
  (.on js/process "SIGTERM" exit))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main [opts & args]
  (if (clojure.string/blank? (:mqtt-url opts))
    (println "MQTT_URL must be given"))
  (reset! opts* opts)
  (listen-on-exit-signals)
  (let [pg (pg/pool opts)
        mqtt-client (mqtt/client opts on-message)]
    (println "pg pool: " pg)
    (println "mqtt-client: " mqtt-client)
    (reset! mqtt-client* mqtt-client)
    (reset! pg* pg)))
