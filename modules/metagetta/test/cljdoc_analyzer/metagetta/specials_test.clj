(ns cljdoc-analyzer.metagetta.specials-test
  "Test various special cases, one at a time"
  (:require
   [cljdoc-analyzer.metagetta.clojurescript :as cljs-reader]
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

(t/deftest analyze-js-default-export-require-test
  ;; Regression: a string require that reaches a JS default/named export via
  ;; the `module$default` form must not break analysis. ClojureScript resolves
  ;; it against the base module, which has to be stubbed in
  ;; :js-dependency-index. https://github.com/cljdoc/cljdoc-analyzer/issues/18
  ;;
  ;; `all-js-requires` is redef'd because it scans the classpath via
  ;; clojure.java.classpath, which doesn't enumerate the test's source
  ;; *directory* under the test runner (it does enumerate jars, the real
  ;; cljdoc input). This feeds in the require the fixture declares.
  (let [ns 'metagetta-test-special.js-default-require]
    (with-redefs [cljs-reader/all-js-requires
                  (constantly #{"some-js-lib/widget$default"})]
      (let [the-ns (->> (analyze-special-source {:languages #{"cljs"}} ns)
                        (#(get % "cljs"))
                        (filter #(= ns (:name %)))
                        first)]
        (t/is (some? the-ns)
              "namespace with a `$default` JS require is analyzed without error")
        (t/is (some #(= 'make-widget (:name %)) (:publics the-ns))
              "its public var is discovered")))))
