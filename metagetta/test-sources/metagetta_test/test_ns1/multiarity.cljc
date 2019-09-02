;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.multiarity)


(defn multiarity
  "Multiarity comment"
  ([] (println "no args"))
  ([a] (println "1 arg" a))
  ([a b] (println "2 args" a b))
  ([a b c d] (println "4 args" a b c d)))
