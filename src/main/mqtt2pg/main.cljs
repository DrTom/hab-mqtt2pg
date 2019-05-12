(ns mqtt2pg.main
  (:require
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [mqtt2pg.run :as run]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))


(def cli-options
  [["-h" "--help"]
   ["-d" "--debug"]
   [nil "--mqtt-url MQTT_URL" "e.g. `MQTT_URL=mqtt://test.mosquitto.org`"
    :default js/process.env.MQTT_URL]
   [nil "--pg-url PG_URL" 
    (str "e.g. `PG_URL=postgresql://USER:SECRET@database.server.com:5432/mydb`;"
         " this can be left unset or blank in which case stardard PG envars will be tried")
    :default js/process.env.PG_URL]
   ])

(defn main-usage [options-summary & more]
  (->> ["mqtt2pg"
        ""
        "usage: mqtt2pg [<opts>] SCOPE/CMD [<scope-opts>] [<args>]"
        ""
        "Available scopes/commands: run" 
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

(defn main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))]
    (if (:debug options) 
      (timbre/set-level! :debug)
      (timbre/set-level! :warn))
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (case (-> arguments first keyword)
              :run (apply run/main (into [options] (rest arguments)))
              (println (main-usage summary {:args args :options options}))))))
