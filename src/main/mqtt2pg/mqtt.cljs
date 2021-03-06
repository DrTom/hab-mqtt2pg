(ns mqtt2pg.mqtt
  (:require
    [mqtt :as mqtt]
    ))

(defn client [opts on-message]
  (let [mqtt-url (:mqtt-url opts)
        options {:username (:mqtt-user opts)
                 :password (:mqtt-password opts)}
        client (mqtt/connect mqtt-url (clj->js options))]
    (.on client "connect" 
         (fn []
           (println "client connected to " mqtt-url)
           (.subscribe client "#" 
                       (fn [err] 
                         (when err
                           (println  "subscribe error " err))))))
    (.on client "end" #(println "mqtt-client end called"))
    (.on client "message" on-message)
    ;(js/setTimeout #(.end client) 1000)
    client))
