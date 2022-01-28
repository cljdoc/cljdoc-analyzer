(ns ^:no-doc metagetta-test.test-ns1.no-doc-wont-load)

;; When excluding namespaces with no-doc metadata,
;; this namespace should not load and therefore not cause analysis failure

(this-unresolved-var-would-cause-load-to-fail)

(also-no-closing-paren-here
