;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.macro)






(defmacro simple
  [a b]
  `(+ ~a ~b))



(defmacro varargs
  [a & xs]
  `(reduce + ~a ~(vec xs)))



(defmacro macdoc
  "Macro docs"
  [a b c d]
  `(+ ~a ~b ~c ~ d))
