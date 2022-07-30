;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.record)

(defrecord DefRecordTest [components])

(def record-test (->DefRecordTest "moodog"))

;; the following are not a result of defrecord factory fn generation and should all be included in API docs
(defn ->NopeNotGeneratedDoc "not a generated positional factory fn, should be included" [])

(defn ->NopeNotGenerated [])

(defn map->NopeNotGeneratedDoc "not a generated map factory fn, should be included" [])

(defn map->NopeNotGenerated [])
