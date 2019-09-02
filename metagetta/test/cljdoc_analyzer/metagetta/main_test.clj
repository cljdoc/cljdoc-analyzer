(ns cljdoc-analyzer.metagetta.main-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.metagetta.main :as main]))

(def base-expected-analysis-result
  '({:name metagetta-test.test-ns1.altered
     :publics [{:name altered-def-with-absolute-file
                :type :var
                :file "metagetta_test/test_ns1/record.cljc"
                :line 7}
               {:name altered-fn-with-source-relative-file
                :arglists ([])
                :type :var
                :file "metagetta_test/test_ns1/multimethod.cljc"
                :line 14}
               {:name altered-macro-with-root-relative-file
                :arglists ([])
                :type :macro
                :doc "added doc\n"
                :file "metagetta_test/test_ns1/multimethod.cljc"
                :line 3}
               {:arglists ([x]),
                :doc "Operation 1 docs\n",
                :file "metagetta_test/test_ns1/protocols.cljc",
                :line 6,
                :name fn-pointing-to-protocol-fn,
                :type :var}]}
    {:name metagetta-test.test-ns1.macro
     :publics [{:name macdoc
                :arglists ([a b c d])
                :type :macro
                :doc "Macro docs\n"
                :file "metagetta_test/test_ns1/macro.cljc"
                :line 23}
               {:name simple
                :arglists ([a b])
                :type :macro
                :file "metagetta_test/test_ns1/macro.cljc"
                :line 11}
               {:name varargs
                :arglists ([a & xs])
                :type :macro
                :file "metagetta_test/test_ns1/macro.cljc"
                :line 17}]}
    {:name metagetta-test.test-ns1.multiarity
     :publics [{:name multiarity
                :arglists ([] [a] [a b] [a b c d])
                :type :var
                :doc "Multiarity comment\n"
                :file "metagetta_test/test_ns1/multiarity.cljc"
                :line 7}]}
    {:name metagetta-test.test-ns1.multimethod
     :publics [{:name start
                :type :multimethod
                :file "metagetta_test/test_ns1/multimethod.cljc"
                :line 6}]}
    {:name metagetta-test.test-ns1.protocols
     :publics [{:name ProtoTest
                :type :protocol
                :doc "Protocol comment.\n"
                :members ({:arglists ([a]), :name alpha :type :var}
                          {:arglists ([z]), :name beta :type :var}
                          {:arglists ([m]), :name matilda :type :var}
                          {:arglists ([x] [x y])
                           :doc "Multi args docs\n"
                           :name multi-args
                           :type :var}
                          {:arglists ([x])
                           :doc "Operation 1 docs\n"
                           :name operation-one,
                           :type :var}
                          {:arglists ([y]), :name zoolander, :type :var})
                :file "metagetta_test/test_ns1/protocols.cljc"
                :line 6}]}
    {:name metagetta-test.test-ns1.record
     :publics [{:name DefRecordTest
                :type :var
                :file "metagetta_test/test_ns1/record.cljc"
                :line 6}
               {:name record-test
                :type :var
                :file "metagetta_test/test_ns1/record.cljc"
                :line 8}]}
    {:name metagetta-test.test-ns1.special-tags
     :publics [{:name added-fn
                :arglists ([a b])
                :type :var
                :added "10.2.2"
                :file "metagetta_test/test_ns1/special_tags.cljc"
                :line 26}
               {:name deprecated-fn
                :arglists ([x])
                :type :var
                :deprecated "0.4.0"
                :file "metagetta_test/test_ns1/special_tags.cljc"
                :line 10}
               {:name dynamic-def
                :type :var
                :doc "dynamic def docs\n"
                :dynamic true
                :file "metagetta_test/test_ns1/special_tags.cljc"
                :line 21}]}))


(def expected-cljs-analysis-result
  (concat
   '({:name metagetta-test.cljs-macro-functions.foo.core
      :publics ({:arglists ([a b])
                 :file "metagetta_test/cljs_macro_functions/foo/core.cljs"
                 :line 6
                 :name add
                 :type :var})}
     {:name metagetta-test.cljs-macro-functions.usage
      :publics ({:arglists ([])
                 :file "metagetta_test/cljs_macro_functions/usage.cljs"
                 :line 19
                 :name example
                 :type :var})})
   base-expected-analysis-result))

(def expected-clj-analysis-result
  (concat
   '({:name metagetta-test.cljs-macro-functions.foo.core,
      :publics ({:arglists ([a b]),
                 :file "metagetta_test/cljs_macro_functions/foo/core.clj",
                 :line 6,
                 :name add,
                 :type :macro})})
   base-expected-analysis-result))

;; TODO: not testing namespaces option yet
(defn- analyze-sources [languages]
  (main/get-metadata {:root-path "test-sources"
                      :languages languages
                      :exclude-with [:no-doc :no-wiki]}))

(t/deftest analyze-cljs-code-test
  (let [actual (analyze-sources #{"cljs"})
        expected {"cljs" expected-cljs-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-code-test
  (let [actual (analyze-sources #{"clj"})
        expected {"clj" expected-clj-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-code-test
  (let [actual (analyze-sources #{"clj" "cljs"})
        expected {"clj" expected-clj-analysis-result
                  "cljs" expected-cljs-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-via-auto-detect-code-test
  (let [actual (analyze-sources :auto-detect)
        expected {"clj" expected-clj-analysis-result
                  "cljs" expected-cljs-analysis-result}]
    (t/is (= expected actual))))
