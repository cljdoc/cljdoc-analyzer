(ns cljdoc-analyzer.metagetta.specials-test
  "Test various special cases, one at a time"
  (:require
   [cljdoc-analyzer.metagetta.main :as main]
   [clojure.test :as t]))

(defn- analyze-special-source
  "Analyze the given namespace from `test-sources-special`.
  Intended for individual tests of special cases."
  [opts namespace]
  (main/get-metadata (merge opts {:root-path "test-sources-special"
                                  :namespaces [namespace]})))

(t/deftest analyze-unknown-tagged-literal-test
  (let [ns 'metagetta-test-special.unknown-tagged-literal
        actual (analyze-special-source
                 {:languages #{"clj" "cljs"}} ns)
        expected-ns (list
                      {:name    ns
                       :publics [{:file "metagetta_test_special/unknown_tagged_literal.cljc"
                                  :line 4
                                  :name 'some-data
                                  :type :var}]})
        expected {"clj" expected-ns
                  "cljs" expected-ns}]
    (t/is (= expected actual))))
