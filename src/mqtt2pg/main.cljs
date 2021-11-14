(ns mqtt2pg.main
  (:require
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [mqtt2pg.db.main :as db]
    [mqtt2pg.exit :as exit]
    [mqtt2pg.logging :as logging]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.proto :as proto]
    [mqtt2pg.run :as run]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [taoensso.timbre :refer-macros [log spy info debug warn error]]
    ))


(defonce args* (atom []))

(def cli-options
  (concat
    [["-h" "--help"]
     ["-d" "--dev-mode" "Keep repl process running do not exit"]
     ]
    mqtt/cli-options
    db/cli-options
    logging/cli-options
    ))

(defn main-usage [options-summary & more]
  (->> ["mqtt2pg"
        ""
        "usage: mqtt2pg [<opts>] SCOPE/CMD [<scope-opts>] [<args>]"
        ""
        "Available scopes/commands: run, proto"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn ^:dev/after-load main []
  (let [args @args*
        {:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))]
    (exit/init options)
    (logging/init options)
    (case (or (some-> arguments first keyword) :help)
      :help (println (main-usage summary {:args args :options options}))
      :run (run/main options (rest arguments))
      :proto (proto/main options (rest arguments))
      )))

(defn -main [& args]
  (info '-main {:args args})
  (reset! args* args)
  (main))
