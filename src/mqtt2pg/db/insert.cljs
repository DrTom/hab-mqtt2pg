(ns mqtt2pg.db.insert
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [mqtt2pg.db.main :as db]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))

(defonce cache* (atom {}))

(defonce wipe-cache-id* (atom nil))

(defn ^:dev/after-load wipe-cache []
  (info "wiping cache")
  (reset! cache* {}))

(defn start-wipe-cache []
  (reset! wipe-cache-id* (js/Date.))
  (go (while true
        (wipe-cache)
        (<! (timeout (* 60 60 1000))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def VALUE-INSERT
  {:name "value-insert"
   :text (str "INSERT INTO events (topic, value, time) "
              "VALUES ($1, $2, $3) RETURNING *")})

(def DATA-INSERT
  {:name "data-insert"
   :text (str "INSERT INTO events (topic, data, time) "
              "VALUES ($1, $2, $3) RETURNING *")})

(def STRING-INSERT
  {:name "string-insert"
   :text (str "INSERT INTO events (topic, data, time) "
              "VALUES ($1, to_json(text($2)), $3) RETURNING *")})

(defn insert* [topic dv & {:keys [ts]
                           :or {ts (js/Date.)}}]
  (when-not @wipe-cache-id* (start-wipe-cache))
  (debug 'insert*)
  (go-try-ch
    (when-not (= dv (get @cache* topic))
      (swap! cache* assoc topic dv)
      (-> (cond
            (number? dv) VALUE-INSERT
            (string? dv) STRING-INSERT
            :else DATA-INSERT)
          (assoc :values [topic dv ts])
          spy db/exec* <?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


