(ns mqtt2pg.main
  (:require
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [mqtt2pg.logging :as logging]
    [mqtt2pg.run :as run]
    [taoensso.timbre :refer-macros [log spy info debug warn error]]
    ))


(defonce args* (atom []))

(def cli-options
  [["-l" "--log-level LOG_LEVEL"
    :default (or (some-> js/process.env.LOG_LEVEL keyword) :info)
    :parse-fn keyword]])

(defn main-usage [options-summary & more]
  (->> ["mqtt2pg"
        ""
        "usage: mqtt2pg [<opts>] SCOPE/CMD [<scope-opts>] [<args>]"
        ""
        "Available scopes/commands: help, run"
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
    (logging/init options)
    (case (or (some-> arguments first keyword) :help)
      :help (println (main-usage summary {:args args :options options}))
      :run (apply run/-main (rest arguments)))))

(defn -main [& args]
  (info '-main {:args args})
  (reset! args* args)
  (main))
