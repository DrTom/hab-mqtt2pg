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
    [mqtt2pg.exit :as exit]
    [mqtt2pg.db.main :as db]
    [mqtt2pg.message-handlers.main :as message_handlers]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error] ]
    ))


(def fs (nodejs/require "fs"))
(defonce opts* (atom {}))


;;; run ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [opts]
  (reset! opts* opts)
  (go (try
        (message_handlers/init)
        (<? (mqtt/init opts mqtt2pg.message-handlers.main/on-message))
        (<? (db/init* opts))
        (catch js/Error e
          (error "aborted run init " e)
          (exit/exit -1)))))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  [["-h" "--help"]])

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


(defn main [gopts args]
  (info 'args args)
  (let [{:keys [arguments
                errors
                options
                summary]} (cli/parse-opts
                            args cli-options
                            :in-order true)
        options (merge gopts options)]

    (if (:help options)
      (do (println (main-usage summary
                               {:args args :options options :arguments arguments}))
          (exit/exit))
      (run options))))
