(ns mqtt2pg.utils.async
  "Extending https://github.com/alexanderkiel/async-error
  with go-try-ch "
  (:require
    #?(:clj [clojure.core.async]
       :cljs [cljs.core.async])
    [taoensso.timbre :as timbre :refer [log spy info debug warn error]]
    ))

;; ---- Helpers Taken from Prismatic Schema -----------------------------------

#?(:clj
   (defn cljs-env?
     "Take the &env from a macro, and tell whether we are expanding into cljs."
     [env]
     (boolean (:ns env))))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for Clojure code.
      https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
     [then else]
     (if (cljs-env? &env) then else)))

;; ---- Helpers ---------------------------------------------------------------

(defn throw-err [e]
  (when (instance? #?(:clj Throwable :cljs js/Error) e) (throw e))
  e)

;; ---- Public API ------------------------------------------------------------

#?(:clj
   (defmacro <?
     "Like <! but throws errors."
     [ch]
     `(if-cljs
        (throw-err (cljs.core.async/<! ~ch))
        (throw-err (clojure.core.async/<! ~ch)))))

#?(:clj
   (defn <??
     "Like <!! but throws errors."
     [ch]
     (throw-err (clojure.core.async/<!! ch))))

#?(:clj
   (defmacro go-try
     "Like go but catches the first thrown error and returns it."
     [& body]
     `(if-cljs
        (cljs.core.async/go
          (try
            ~@body
            (catch js/Error e# e#)))
        (clojure.core.async/go
          (try
            ~@body
            (catch Throwable t# t#))))))

#?(:clj
   (defmacro go-try-ch
     "Like go and similar to go-try but returns a chanel;
     either the result of body (or false if the result is nil) will be put
     on the channel or the exception thrown when executing body."
     [& body]
     `(if-cljs
        (let [ch# (cljs.core.async/chan)]
          (cljs.core.async/go
            (try
              (cljs.core.async/>! ch# (or ~@body false))
              (catch js/Error e#
                ;(taoensso.timbre/warn e#)
                (cljs.core.async/>! ch# e#))))
          ch#)
        (let [ch# (clojure.core.async/chan)]
          (clojure.core.async/go
            (try
              (clojure.core.async/>! ch# (or ~@body false))
              (catch Throwable th#
                ;(taoensso.timbre/warn th#)
                (clojure.core.async/>! ch# th#))))
          ch#))))
