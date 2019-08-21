;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns cljdoc-analyzer-test.multiarity)


(defn multiarity
  "Multiarity comment"
  ([] (println "no args"))
  ([a] (println "1 arg"))
  ([a b] (println "2 args"))
  ([a b c d] (println "4 args")))
