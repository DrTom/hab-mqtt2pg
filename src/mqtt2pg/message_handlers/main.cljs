(ns mqtt2pg.message-handlers.main
  (:require
    [clojure.string :as s]
    [mqtt2pg.config :as config]
    [mqtt2pg.message-handlers.ventilation :as ventilation-handler]
    [mqtt2pg.message-handlers.generic-state :as generic-state-handler]
    [mqtt2pg.message-handlers.hue :as hue-handler]
    [mqtt2pg.message-handlers.shelly :as shellies-handler]
    [mqtt2pg.utils.core :refer [presence]]
    [taoensso.timbre :as timbre :refer-macros [log spy info debug warn error]]))

(defn on-message [topic _message]
  (let [message (.toString _message)]
    (condp #(s/starts-with? %2 %1) topic
      "ventilation" (ventilation-handler/on-message topic message)
      "shellies/" (shellies-handler/on-message topic message)
      "shellies/shellyht" (generic-state-handler/on-message topic message)
      "hue/" (hue-handler/on-message topic message)
;      "" (generic-state-handler/on-message topic message)
      (warn "Unhandled topic: " {:topic topic :message message}))))

(defn init []
  (ventilation-handler/init))
