(ns mqtt2pg.config)


(def LOGGING-DEFAULTS
  {:min-level [[#{
                  ;"mqtt2pg.db.*"
                  ;"mqtt2pg.proto"
                  } :debug]
               [#{"mqtt2pg.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})

(def mqtt-client* (atom nil))
(def opts* (atom nil))



