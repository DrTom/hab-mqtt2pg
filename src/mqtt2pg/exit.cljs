(ns mqtt2pg.exit
  (:require
    [cljs.nodejs :as nodejs]
    [taoensso.timbre :refer [debug info warn error]]))

(def opts* (atom {}))

(defn exit
  ([] (exit 0))
  ([v]
   (if (:dev-mode @opts*)
     (warn "exit ignored in dev-mode")
     (js/setTimeout #(js/process.exit v) 500))))

(defn listen-on-exit-signals []
  (.on js/process "SIGINT" #(exit 0))
  (.on js/process "SIGTERM" #(exit 0)))

(defn init [options]
  (reset! opts* options)
  (listen-on-exit-signals))
