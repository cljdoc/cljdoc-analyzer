;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.record)

(defrecord DefRecordTest [components])

(def record-test (->DefRecordTest "moodog"))
