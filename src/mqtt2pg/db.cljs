(ns mqtt2pg.db
  (:require
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [mqtt2pg.utils.core :refer [presence]]
    ["pg" :as pg]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))

(defonce pool* (atom nil))

(defn deinit []
  (when-let [pool @pool*]
    (reset! pool* nil)
    (go (<p! (.end pool)))
    (debug "Sent DB-POOL termination " pool)))


(defn init [opts]
  (let [conn-opts (get opts :pg-url {})
        ch (async/chan)]
    (deinit)
    (info "PG-POOL Initializing ... " conn-opts)
    (go (try
          (let [pool (pg/Pool. conn-opts)
                res (<p! (.query pool "SELECT NOW()"))]
            (debug "connection test " (js->clj (.-rows res)))
            (reset! pool* pool)
            (info "PG-POOL Initalized " pool)
            (>! ch pool))
          (catch js/Error e
            (error "PG-POOL Initialization error " e)
            (>! ch e))))
    ch))

