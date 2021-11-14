(ns mqtt2pg.db.main
  (:require
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [applied-science.js-interop :as j]
    [mqtt2pg.utils.core :refer [presence]]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    ["pg" :as pg]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))

(defonce pool* (atom nil))

(def cli-options
  [[nil "--pg-url PG_URL"
    (str "e.g. `PG_URL=postgresql://USER:SECRET@database.server.com:5432/mydb`;"
         " this can be left unset or blank in which case stardard PG envars will be tried")
    :default js/process.env.PG_URL]])

(defn deinit []
  (when-let [pool @pool*]
    (reset! pool* nil)
    (go (<p! (.end pool)))
    (debug "Sent DB-POOL termination " pool)))


(defn init* [opts]
  (deinit)
  (go-try-ch
    (info "PG-POOL initializing ...")
    (let [pool (pg/Pool. (clj->js (get opts :pg-url {:connectionTimeoutMillis 1000})))
          res (<p! (.query pool "SELECT NOW()"))]
      (reset! pool* pool)
      (info "PG-POOL Initalized " (js->clj (.-rows res))
            {:total (j/get pool :totalCount)
             :idle (j/get pool :idleCount)
             :waiting (j/get pool :waitingCount)})
      pool)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-statement
  [text values & {:keys [client]
                  :or {client @pool*}}]
  (js/Promise.
    (fn [p-resolve, p-reject]
      (go (try (some-> (.query client text (clj->js values))
                       <p! (j/get :rows) js->clj p-resolve)
               (catch js/Error e
                 (warn "QUERY ERROR" e)
                 (p-reject (ex-cause  e))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce last-persisted-events* (atom {}))

(def INSERT-NUMBER-EVENT-STATEMENT
  "INSERT INTO number_events (topic, value) VALUES ($1, $2) RETURNING *")

(def INSERT-TEXT-EVENT-STATEMENT
  "INSERT INTO text_events (topic, value) VALUES ($1, $2) RETURNING *")

(def INSERT-DATA-EVENT-STATEMENT
  "INSERT INTO data_events (topic, value) VALUES ($1, $2) RETURNING *")

(defn insert-event
  [topic value & {:keys [client]
                  :or {client @pool*}}]
  (if (= (get @last-persisted-events* topic) value)
    (debug "skipping inserting same value" topic value)
    (go (try
          (let [statement (cond
                            (number? value) INSERT-NUMBER-EVENT-STATEMENT
                            (string? value) INSERT-TEXT-EVENT-STATEMENT
                            :else INSERT-DATA-EVENT-STATEMENT)
                rows (<p! (query-statement
                            statement [topic value]
                            :client client))]
            (if (empty? rows)
              (warn "nothing inserted "
                    statement [topic value])
              (do (debug "inserted " rows)
                  (swap! last-persisted-events*
                         assoc topic value))))
          (catch js/Error e (error e))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn root-cause [e]
  (if-let [c (ex-cause e)]
    (root-cause c)
    e))

(defn exec*
  [& args]
  (go-try-ch
    (try (some-> (j/apply @pool* :query (clj->js args))
                 <p! (j/get :rows) js->clj)
         (catch js/Error e
           (warn "QUERY ERROR in exec2* " e)
           (throw (root-cause e))))))

