(ns mqtt2pg.mqtt
  (:require
    [async-error.core :refer-macros [<?]]
    [async-mqtt]
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [clojure.set :refer [rename-keys]]
    [mqtt :as mqtt]
    [taoensso.timbre :refer [debug info warn error spy]]
    ))

(defonce client* (atom nil))

(defn deinit []
  (when-let [client @client*]
    (debug "MQTT deinitializing " client)
    (reset! client* nil)
    (go (<p! ^js/Promise (.end client)))))

(defn set-client-cbs [client on-message]
  ^js/Object (.on client "message" (fn [& args]
                          (debug "on-message" args)
                          (apply on-message args)))
  ^js/Object (.on client "end" #(debug "client ended"))
  nil)

(defn init [opts on-message]
  (info "MQTT Initializing ...")
  (deinit)
  (let [ch (chan)]
    (go
      (try
        (let [client (<p! (async-mqtt/connectAsync
                            (:mqtt-url opts)
                            (-> opts
                                (select-keys [:mqtt-user :mqtt-password])
                                (rename-keys {:mqtt-user :username
                                              :mqtt-password :password})
                                clj->js)))]
          (reset! client* client)
          (set-client-cbs client on-message)
          (<p! (.subscribe client "#"))
          (>! ch client)
          (info "MQTT Ininitialized "))
        (catch js/Error e
          (error "MQTT Initialization error " e)
          (>! ch e))))
    ch))
