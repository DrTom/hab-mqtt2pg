(ns mqtt2pg.utils.cli
  (:refer-clojure :exclude [str keyword])
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as tools-cli]
    [cuerdas.core :as string :refer [lower split]]
    [schank.dbsweep.utils.core :refer [str keyword]]
    [taoensso.timbre :as timbre :refer [debug info warn error]]))

(def parse-opts tools-cli/parse-opts)

(defn usage [options-sumary commands & more]
  (->> ["dbsweep"
        ""
        "usage: dbsweep [<opts>] SCOPE|CMD [<scope-opts>] SCOPE|CMD [<scope-opts>] ..."
        ""
        (str "Available scopes/commands: " (->> commands
                                                (map str)
                                                (string/join ", ")))
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
