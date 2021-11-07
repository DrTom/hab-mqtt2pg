(ns mqtt2pg.message-handlers.shelly
  (:require
    [mqtt2pg.config :as config]
    [mqtt2pg.utils.core :refer [presence]]
    [mqtt2pg.db :as db]
    [clojure.string :as s]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))

(def devices* (atom nil))

(defn state-pattern [device]
  (re-pattern
    (str "^shellies/" (:type device) "-" (:id device) "/roller/0/(.+)$")))

(defn name-device [topic]
  (reduce (fn [t device]
            (s/replace t (state-pattern device) (str (:name device) "/$1"))
            ) topic @devices*))


(defn topicmapper [topic]
  (-> topic
      (s/replace #"^(.*)/roller/0$" "$1/roller/0/action")
      (name-device)))

(def last-persisted-values* (atom {}))

(defn on-message [topic message]
  (debug 'on-message {:topic topic :message message})
  (let [db-topic (topicmapper topic)
        event (clojure.string/replace-first db-topic #"^(.*)/" "")
        [table, value] (case event
                         ("pos" "power" "energy") ["number_events" (js/parseFloat message)]
                         "action" ["text_events", message]
                         [nil, nil])]
    (if-not table
      (warn "unpersisted message" {:table table :value value :event event :db-topic db-topic :topic topic :message message})
      (if (= value (get @last-persisted-values* db-topic))
        (info "skip persisting same value" db-topic value)
        (.query @db/pool* (clj->js
                            {:name table
                             :text (str "INSERT INTO " table " (topic, value) "
                                        "VALUES ($1, $2)")
                             :values [db-topic, value]})
                (fn [err, res]
                  (if err
                    (error err)
                    (swap! last-persisted-values* assoc db-topic value))))))))

(defn read-devices []
  (.query @db/pool* (clj->js {:text "SELECT * FROM devices"  ;WHERE type = 'shellyswitch25'
                              :rowMode 'array'})
          (fn [err,res]
            (reset! devices* (some->> res .-rows seq
                                      (map js->clj)
                                      (map clojure.walk/keywordize-keys)
                                      doall))
            (info "updated devices" @devices*)))
  (js/setTimeout read-devices (* 60 1000)))

(defn init []
  (read-devices))
