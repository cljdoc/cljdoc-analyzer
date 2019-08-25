;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns cljdoc-analyzer-test.macro)






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
