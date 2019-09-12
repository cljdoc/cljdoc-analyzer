;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.cljs-macro-functions.usage
  (:require-macros [metagetta-test.cljs-macro-functions.foo.core])
  (:require [metagetta-test.cljs-macro-functions.foo.core]))

;; Apparently in cljs a macro and a function can have the same name.
;; from https://blog.fikesfarm.com/posts/2015-12-12-clojurescript-macro-functions.html

;; Reading this:

;; "The doc and source functions display content from the function—this behavior
;; is consistent with the notion that the macro is akin to a performance-related
;; implementation detail."

;; strongly implies that only the fn version will be exposed by the cljs analyzer.

(defn example []
  ;; calls the function
  (metagetta-test.cljs-macro-functions.foo.core/add 1 2)
  ;; calls the macro
  (apply metagetta-test.cljs-macro-functions.foo.core/add [1 2]))
