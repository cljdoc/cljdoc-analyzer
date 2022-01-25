(ns metagetta-test.test-ns1.no-doc-wont-load2 "Should not be analyzed" {:no-doc true})

;; When excluding namespaces with no-doc metadata,
;; this namespace should not load and therefore not cause analysis failure

(this-unresolved-var-would-cause-load-to-fail)
