;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.special-tags)





(defn  ^{:deprecated "0.4.0"} deprecated-fn[x] x)





(defn ^:no-doc dont-doc-me "no docs please" [x] x)




(def ^:dynamic dynamic-def "dynamic def docs" 42)




(defn ^{:added "10.2.2"} added-fn[a b] (+ a b))
