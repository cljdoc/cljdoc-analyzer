(ns cljdoc-analyzer.metagetta.main-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.metagetta.main :as main]))

(def expected-analysis-result
  '({:name cljdoc-analyzer-test.altered
    :publics [{:name altered-def-with-absolute-file
               :type :var
               :file "cljdoc_analyzer_test/record.cljc"
               :line 7}
              {:name altered-fn-with-source-relative-file
               :arglists ([])
               :type :var
               :file "cljdoc_analyzer_test/multimethod.cljc"
               :line 14}
              {:name altered-macro-with-root-relative-file
               :arglists ([])
               :type :macro
               :doc "added doc\n"
               :file "cljdoc_analyzer_test/multimethod.cljc"
               :line 3}
              {:arglists ([x]),
               :doc "Operation 1 docs\n",
               :file "cljdoc_analyzer_test/protocols.cljc",
               :line 6,
               :name fn-pointing-to-protocol-fn,
               :type :var}]}
   {:name cljdoc-analyzer-test.macro
    :publics [{:name macdoc
               :arglists ([a b c d])
               :type :macro
               :doc "Macro docs\n"
               :file "cljdoc_analyzer_test/macro.cljc"
               :line 23}
              {:name simple
               :arglists ([a b])
               :type :macro
               :file "cljdoc_analyzer_test/macro.cljc"
               :line 11}
              {:name varargs
               :arglists ([a & xs])
               :type :macro
               :file "cljdoc_analyzer_test/macro.cljc"
               :line 17}]}
   {:name cljdoc-analyzer-test.multiarity
    :publics [{:name multiarity
               :arglists ([] [a] [a b] [a b c d])
               :type :var
               :doc "Multiarity comment\n"
               :file "cljdoc_analyzer_test/multiarity.cljc"
               :line 7}]}
   {:name cljdoc-analyzer-test.multimethod
    :publics [{:name start
               :type :multimethod
               :file "cljdoc_analyzer_test/multimethod.cljc"
               :line 6}]}
   {:name cljdoc-analyzer-test.protocols
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
               :file "cljdoc_analyzer_test/protocols.cljc"
               :line 6}]}
   {:name cljdoc-analyzer-test.record
    :publics [{:name DefRecordTest
               :type :var
               :file "cljdoc_analyzer_test/record.cljc"
               :line 6}
              {:name record-test
               :type :var
               :file "cljdoc_analyzer_test/record.cljc"
               :line 8}]}
   {:name cljdoc-analyzer-test.special-tags
    :publics [{:name added-fn
               :arglists ([a b])
               :type :var
               :added "10.2.2"
               :file "cljdoc_analyzer_test/special_tags.cljc"
               :line 26}
              {:name deprecated-fn
               :arglists ([x])
               :type :var
               :deprecated "0.4.0"
               :file "cljdoc_analyzer_test/special_tags.cljc"
               :line 10}
              {:name dynamic-def
               :type :var
               :doc "dynamic def docs\n"
               :dynamic true
               :file "cljdoc_analyzer_test/special_tags.cljc"
               :line 21}]}))

;; TODO: not testing namespaces option yet
;; TODO: only currently testing cljc, include clj and cljs
(defn- analyze-sources [languages]
  (main/get-metadata {:root-path "test-sources"
                      :languages languages}))

(t/deftest analyze-cljs-code-test
  (let [actual (analyze-sources #{"cljs"})
        expected {"cljs" expected-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-code-test
  (let [actual (analyze-sources #{"clj"})
        expected {"clj" expected-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-code-test
  (let [actual (analyze-sources #{"clj" "cljs"})
        expected {"clj" expected-analysis-result
                  "cljs" expected-analysis-result}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-via-auto-detect-code-test
  (let [actual (analyze-sources :auto-detect)
        expected {"clj" expected-analysis-result
                  "cljs" expected-analysis-result}]
    (t/is (= expected actual))))
