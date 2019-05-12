(ns mqtt2pg.pg
  (:require
    [pg]
    [mqtt2pg.utils :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))


(defn pool [opts]
  (let [conn-opts (if-let [url (-> opts :pg-url presence)]
                    {:connectionString url}
                    {})]
    (debug "Initializing pg/Pool. " conn-opts)
    (pg/Pool. (clj->js conn-opts))))
