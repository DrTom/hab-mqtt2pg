(ns mqtt2pg.utils.sql
  (:require
    [cuerdas.core :refer [split trim]]
    [applied-science.js-interop :as j]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]
    ))

(defn honey->pg [[query & params]]
  (let [c* (atom 0)
        res-query (-> query
                      (str " "); ? could be the last char
                      (split "?")
                      (->> (reduce
                             (fn [s1 s2]
                               (swap! c* inc)
                               (str s1 "$" @c* s2))))
                      trim)]
    (debug query res-query @c* params)
    (when-not (= @c* (count params))
      (throw (js/Error. "The query contains a `?` question mark (maybe an operator); this is not supported")))
    [res-query params]))


