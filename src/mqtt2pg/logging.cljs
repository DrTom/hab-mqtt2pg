(ns mqtt2pg.logging
  (:require
    [cljs.nodejs :as nodejs]
    [taoensso.timbre :as timbre :refer [debug warn info error]]
    ))

(def fs (nodejs/require "fs"))

(defn init [options]
  (info "initializing logging ..." options)
  (timbre/set-level! (-> options :log-level keyword))
  (when-let [logging-config-file (:logging-config-file options)]
    (let [config (str ^js/Object (.readFileSync fs logging-config-file))]
      (timbre/merge-config! (cljs.reader/read-string config))
      (debug 'logging-config timbre/*config*)))
  (info "initizlized logging " timbre/*config* ))
