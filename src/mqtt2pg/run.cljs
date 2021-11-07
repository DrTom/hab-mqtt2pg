(ns mqtt2pg.run
  (:require
    [async-error.core :refer-macros [<?]]
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [clojure.walk]
    [mqtt2pg.config :as config]
    [mqtt2pg.db :as db]
    [mqtt2pg.message-handlers.main :as message_handlers]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error] ]
    ))


(def fs (nodejs/require "fs"))
(defonce opts* (atom {}))

;;; exit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exit [v]
  (db/deinit)
  (mqtt/deinit)
  (js/setTimeout #(js/process.exit v) 500))

(defn listen-on-exit-signals []
  (.on js/process "SIGINT" (partial exit 0))
  (.on js/process "SIGTERM" (partial exit 0)))


;;; run ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [opts]
  (reset! opts* opts)
  (go (try
        (listen-on-exit-signals)
        (message_handlers/init)
        (<? (db/init opts))
        (<? (mqtt/init opts mqtt2pg.message-handlers.main/on-message))
        (catch js/Error e
          (error "aborted init " e)
          (exit -1)))))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  [["-h" "--help"]
   ["-l" "--log-level LOG_LEVEL"
    :default (or (some-> js/process.env.LOG_LEVEL keyword) :info)
    :parse-fn keyword]
   [nil "--logging-config-file LOGGING_CONFIG_FILE"
    :default (or js/process.env.LOGGING_CONFIG_FILE)]
   [nil "--mqtt-url MQTT_URL" "e.g. `MQTT_URL=mqtt://test.mosquitto.org`"
    :default (or js/process.env.MQTT_URL "tcp://localhost:1883")]
   [nil "--mqtt-user MQTT_USER" "user(name) for connecting to the mqtt server"
    :default js/process.env.MQTT_USER]
   [nil "--mqtt-password MQTT_PASSWORD" "password for connecting to the mqtt server"
    :default js/process.env.MQTT_PASSWORD]
   [nil "--pg-url PG_URL"
    (str "e.g. `PG_URL=postgresql://USER:SECRET@database.server.com:5432/mydb`;"
         " this can be left unset or blank in which case stardard PG envars will be tried")
    :default js/process.env.PG_URL]])

(defn main-usage [options-sumary & more]
  (->> ["mqtt2pg"
        ""
        "usage: mqtt2pg run [CMD] [<run-opts>]"
        ""
        "Available commands: help"
        ""
        "Options:"
        options-sumary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))


(defn -main [& args]
  (info 'args args)
  (let [{:keys [arguments
                errors
                options
                summary]} (cli/parse-opts
                            args cli-options
                            :in-order true)]
    (when-let [logging-config-file (:logging-config-file options)]
      (let [config (str ^js/Object (.readFileSync fs logging-config-file))]
        (timbre/merge-config! (cljs.reader/read-string config))
        (info 'logging-config timbre/*config*)))
    (let [command (some-> arguments first keyword)]
      (case command
        :help (do (println (main-usage summary {:args args :options options :arguments arguments}))
                  (exit 0))
        nil (run options)
        (do (println (main-usage summary
                                 {:args args :options options :arguments arguments}
                                 (str "unknown command "command)))
            (exit 0))))))
