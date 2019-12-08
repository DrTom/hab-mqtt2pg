(ns mqtt2pg.pg
  (:require
    [pg]
    [mqtt2pg.utils :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))


(def pool* (atom nil))

(defn terminate []
  (when-let [pool @pool*]
    (.end pool)
    (info "DB-POOL terminated" @pool*)
    (reset! pool* nil)))

(defn initialize [opts]
  (terminate)
  (let [conn-opts (if-let [url (-> opts :pg-url presence)]
                    {:connectionString url}
                    {})]
    (debug "Initializing pg/Pool. " conn-opts)
    (reset! pool* (pg/Pool. (clj->js conn-opts)))
    (info "DB-POOL initalized" @pool*)))



