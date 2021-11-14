(ns mqtt2pg.proto
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as async :refer [go <! >! put! chan timeout]]
    [cljs.core.async.interop :refer [<p!]]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [clojure.walk]
    [mqtt2pg.config :as config]
    [mqtt2pg.db.main :as db]
    [mqtt2pg.db.insert :as db-insert]
    [mqtt2pg.mqtt :as mqtt]
    [mqtt2pg.utils.async :refer-macros [<? go-try-ch]]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error] ]
    ))


(defn stuff []
  (.query @db/pool* "INSERT INTO events (topic, value) VALUES ($1 ,$2)"
          (clj->js ["test" 123])
          (fn [err, res]
            (info res)
            (error err)))
  (comment (let [res (<? (db-insert/insert* "test" 123))]
             ;(throw (js/Error. "blah"))
             (info 'res res)
             )))

(defn test-db-query-statement* []
  (go-try-ch
    (try
      (let [res  (<p! (db/query-statement "SELECT true" []))]
        (info 'res res)
        res)
      (catch js/Error e
        (info e)))))

(defn test-db-query-statement-error* []
  (go-try-ch
    (try
      (let [res  (<p! (db/query-statement "SELECT * FROM no_such_table" []))]
        (info 'res res)
        res)
      (catch js/Error e
        (info (ex-message (ex-cause e)))))))

(defn test-db-exec* []
  (go-try-ch
    (try
      (let [res  (<? (db/exec* "SELECT true" []))]
        (info 'res res)
        res)
      (catch js/Error e
        (error e)))))

(defn test-db-exec-error* []
  (go-try-ch
    (try
      (let [res  (<? (db/exec* "SELECT * FROM no_such_table" []))]
        res)
      (catch js/Error e
        (info e)))))

(defn test-number-insert* []
  (go-try-ch
    (-> (db-insert/insert* "test_num" 123)
        <? spy)))

(defn test-bool-insert* []
  (go-try-ch
    (-> (db-insert/insert* "test_bool" true)
        <? spy)))

(defn test-map-insert* []
  (go-try-ch
    (-> (db-insert/insert* "test_map" {:x 42 :y false})
        <? spy)))

(defn test-string-insert* []
  (go-try-ch
    (-> (db-insert/insert* "test_str" "foo")
        <? spy)))

(defn main [options args]
  (info 'proto/main {:options options :args args})
  (go-try-ch
    (let [_ (<? (db/init* options))
          ;_ (<! (test-db-query-statement*))
          ;_ (<! (test-db-query-statement-error*))
          ;_ (<! (test-db-exec*))
          ;_ (<! (test-db-exec-error*))
          _ (<? (test-number-insert*))
          _ (<? (test-bool-insert*))
          _ (<? (test-map-insert*))
          _ (<? (test-string-insert*))])))
