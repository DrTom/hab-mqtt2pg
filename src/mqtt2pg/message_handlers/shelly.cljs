(ns mqtt2pg.message-handlers.shelly
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [clojure.string :as s]
    [cuerdas.core :as string :refer [lower]]
    [mqtt2pg.config :as config]
    [mqtt2pg.db.insert :as db-insert :refer [insert*]]
    [mqtt2pg.db.main :as db]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ROLLERS-POSITION-UPDATE-STATEMENT
  {:name "update-roller-position"
   :text "UPDATE rollers SET position = $1 WHERE device_id = $2 RETURNING *"})

(defn update-roller-position [shelly-id value]
  (go (try (let [rows (-> ROLLERS-POSITION-UPDATE-STATEMENT
                          (assoc :values [value shelly-id])
                          db/exec* <?)]
             (if (empty? rows)
               (warn "roller not updated, missing shelly-id: " shelly-id)
               (debug "updated " rows)))
           (catch js/Error e (error e)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch-shellyswitch25 [shelly-id path topic message]
  (go (try (case path
             "roller/0/pos" (let [v (js/parseInt message)]
                              (update-roller-position shelly-id v)
                              (<? (insert* topic v)))
             ("temperature"
               "voltage") (<? (insert*
                                topic
                                (-> message js/parseFloat js/Math.round)))
             ("input/0"
               "input/1"
               "overtemperature") (<? (insert* topic (= "1" message)))
             ("roller/0/stop_reason"
              "temperature_status") (<? (insert* topic (lower message)))
             ("relay/energy"
               "relay/power"
               "roller/0" ; start|stop
               "roller/0/energy"
               "roller/0/power"
                ; normal|...
               "temperature_f") (debug "ignoring:"
                                       {:path path :message message})
             (warn "TODO shellyswitch25 " {:path path :message message}))

           (catch js/Error e
             (warn (ex-message e) [topic message])))))


(defn on-message [topic message]
  (debug 'on-message {:topic topic :message message})
  (try
    (let [[_ shellytype
           shelly-id path] (re-find
                             #"shellies/([a-zA-Z0-9]+)-([a-zA-Z0-9]+)/(.*)"
                             topic)]
      (case shellytype
        "shellyswitch25" (dispatch-shellyswitch25 shelly-id path topic message)
        (warn "TODO shellytype" shellytype)))
    (catch js/Error e
      (error e))))

(defn init [])

