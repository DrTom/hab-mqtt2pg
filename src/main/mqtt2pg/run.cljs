(ns mqtt2pg.run
  (:require
    [cljs-await.core :refer [await]]
    [cljs.core.async :as async :refer [<! >! put! chan timeout]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [clojure.walk]
    [mqtt2pg.config :as config]
    [mqtt2pg.message-handlers.main :as message_handlers]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.pg :as pg]
    [mqtt2pg.utils :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error] ]
    ))



;;; message handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;; exit handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exit []
  (pg/terminate)
  (when-let [mqtt-client @config/mqtt-client*]
    (.end mqtt-client))
  (js/setTimeout #(js/process.exit 0) 500))

(defn listen-on-exit-signals []
  (.on js/process "SIGINT" exit)
  (.on js/process "SIGTERM" exit))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; TODO await pg connection
; https://node-postgres.com/api/pool


(defn main [opts & args]
  (if (clojure.string/blank? (:mqtt-url opts))
    (println "MQTT_URL must be given"))
  (reset! config/opts* opts)
  (listen-on-exit-signals)
  (pg/initialize opts)
  (let [mqtt-client (mqtt/client opts mqtt2pg.message-handlers.main/on-message)]
    (message_handlers/init)
    (println "mqtt-client: " mqtt-client)
    (reset! config/mqtt-client* mqtt-client)
    ))
