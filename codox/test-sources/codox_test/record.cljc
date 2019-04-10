;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns codox-test.record)

(defrecord DefRecordTest [components])

(def record-test (->DefRecordTest "moodog"))
