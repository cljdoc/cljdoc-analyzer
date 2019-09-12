;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.cljs-macro-functions.foo.core)

(defmacro add [a b]
  `(+ ~a ~b))
