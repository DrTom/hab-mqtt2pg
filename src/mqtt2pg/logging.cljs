(ns mqtt2pg.logging
  (:require
    [cljs.nodejs :as nodejs]
    [mqtt2pg.config :as config]
    [taoensso.timbre :as timbre :refer [debug warn info error spy]]
    ))

(def cli-options
  [[nil "--logging-config-file LOGGING_CONFIG_FILE"
    :default (or js/process.env.LOGGING_CONFIG_FILE)]])

(def fs (nodejs/require "fs"))

(defn read-file [path]
  (debug 'read-file path)
  (let [config (str ^js/Object (.readFileSync fs path))]
    (spy (cljs.reader/read-string config))))

(defn init [options]
  (info "initializing logging ..." options)
  (timbre/merge-config! config/LOGGING-DEFAULTS)
  (when-let [logging-config (:logging-config options)]
    (timbre/merge-config! logging-config))
  (info "initizlized logging " timbre/*config* ))
