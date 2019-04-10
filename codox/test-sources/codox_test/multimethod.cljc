;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns codox-test.multimethod)

(defmulti start (fn [k opts] k))

(defmethod start :car [_ opts]
  (println "Starting car..." opts))

(defmethod start :helicopter [_ opts]
  (println "Starting helicopter..." opts))
