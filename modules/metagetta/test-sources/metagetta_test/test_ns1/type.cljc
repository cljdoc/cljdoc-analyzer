;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.type)

(deftype DefTypeTest [one two])

(def type-test (-> (->DefTypeTest "moo" "dog")))

;; the following are not a result of deftype factory fn generation and should all be included in API docs
(defn ->NotGeneratedDoc "not generated and should be included" [])

(defn ->NotGenerated [])
