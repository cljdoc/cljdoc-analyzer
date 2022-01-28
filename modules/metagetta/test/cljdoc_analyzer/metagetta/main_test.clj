(ns cljdoc-analyzer.metagetta.main-test
  "Load all `test-sources/*` namespaces and test various things about them."
  (:require [clojure.test :as t]
            [cljdoc-analyzer.metagetta.main :as main]))

(defn- in? [coll elem]
  (some #(= elem %) coll))

(defn- concat-res [& args]
  (remove nil? (apply concat args)))

(defn- expected-result [& opts]
  (concat-res
   (when (in? opts :clj)
     (list
      {:name 'metagetta-test.cljs-macro-functions.foo.core,
       :publics [{:arglists '([a b]),
                  :file "metagetta_test/cljs_macro_functions/foo/core.clj",
                  :line 6,
                  :name 'add,
                  :type :macro}]}))
   (when (in? opts :cljs)
     (list
      {:name 'metagetta-test.cljs-macro-functions.foo.core
       :publics [{:arglists '([a b])
                  :file "metagetta_test/cljs_macro_functions/foo/core.cljs"
                  :line 6
                  :name 'add
                  :type :var}]}
      {:name 'metagetta-test.cljs-macro-functions.usage
       :publics [{:arglists '([])
                  :file "metagetta_test/cljs_macro_functions/usage.cljs"
                  :line 19
                  :name 'example
                  :type :var}]}))
   (list {:name 'metagetta-test.test-ns1.altered
          :publics [{:name 'altered-def-with-absolute-file
                     :type :var
                     :file "metagetta_test/test_ns1/record.cljc"
                     :line 7}
                    {:name 'altered-fn-with-source-relative-file
                     :arglists '([])
                     :type :var
                     :file "metagetta_test/test_ns1/multimethod.cljc"
                     :line 14}
                    {:name 'altered-macro-with-root-relative-file
                       :arglists '([])
                     :type :macro
                     :doc "added doc\n"
                     :file "metagetta_test/test_ns1/multimethod.cljc"
                     :line 3}
                    {:arglists '([x]),
                     :doc "Operation 1 docs\n",
                     :file "metagetta_test/test_ns1/protocols.cljc",
                     :line 6,
                     :name 'fn-pointing-to-protocol-fn,
                     :type :var}]}
         {:name 'metagetta-test.test-ns1.macro
          :publics [{:name 'macdoc
                     :arglists '([a b c d])
                     :type :macro
                     :doc "Macro docs\n"
                     :file "metagetta_test/test_ns1/macro.cljc"
                     :line 23}
                    {:name 'simple
                     :arglists '([a b])
                     :type :macro
                     :file "metagetta_test/test_ns1/macro.cljc"
                     :line 11}
                    {:name 'varargs
                     :arglists '([a & xs])
                     :type :macro
                     :file "metagetta_test/test_ns1/macro.cljc"
                     :line 17}]}
         {:name 'metagetta-test.test-ns1.multiarity
          :publics [{:name 'multiarity
                     :arglists '([] [a] [a b] [a b c d])
                     :type :var
                     :doc "Multiarity comment\n"
                       :file "metagetta_test/test_ns1/multiarity.cljc"
                     :line 7}]}
         {:name 'metagetta-test.test-ns1.multimethod
          :publics [{:name 'start
                     :type :multimethod
                     :file "metagetta_test/test_ns1/multimethod.cljc"
                     :line 6}]})
   (when (not (in? opts :no-doc))
     (list
       {:doc "This namespace will be marked with no-doc at load-time\n"
        :name 'metagetta-test.test-ns1.no-doc-me-later
        :no-doc true
        :publics [{:name 'some-var
                   :arglists '([x])
                   :file "metagetta_test/test_ns1/no_doc_me_later.cljc",
                   :line 9
                   :type :var}]}
       {:name 'metagetta-test.test-ns1.no-doc-ns
        :no-doc true
        :publics [{:name 'not-documented
                   :arglists '([a])
                   :type :var
                   :file "metagetta_test/test_ns1/no_doc_ns.cljc"
                   :line 6}]}))
   (list
    {:name 'metagetta-test.test-ns1.protocols
     :publics [{:name 'ProtoTest
                :type :protocol
                :doc "Protocol comment.\n"
                :members '({:arglists ([a]) :name alpha :type :var}
                           {:arglists ([z]) :name beta :type :var}
                           {:arglists ([m]) :name matilda :type :var}
                           {:arglists ([x] [x y])
                            :doc "Multi args docs\n"
                            :name multi-args
                            :type :var}
                           {:arglists ([x])
                            :doc "Operation 1 docs\n"
                            :name operation-one,
                            :type :var}
                           {:arglists ([y]) :name zoolander :type :var})
                :file "metagetta_test/test_ns1/protocols.cljc"
                :line 6}]}
    {:name 'metagetta-test.test-ns1.record
     :publics [{:name 'DefRecordTest
                :type :var
                :file "metagetta_test/test_ns1/record.cljc"
                :line 6}
               {:name 'record-test
                :type :var
                :file "metagetta_test/test_ns1/record.cljc"
                :line 8}]})
   (when (not (in? opts :skip-wiki))
     (list
      {:name 'metagetta-test.test-ns1.skip-wiki-ns
       :doc "skip-wiki is legacy for autodoc\n"
       :skip-wiki true
       :publics [{:name 'not-wikied
                  :arglists '([b])
                  :type :var
                  :file "metagetta_test/test_ns1/skip_wiki_ns.cljc"
                  :line 6}]}))
   (list
    {:name 'metagetta-test.test-ns1.special-tags
     :doc "document the special tags namespace\n"
     :author "Respect my authoritah"
     :added "0.1.1"
     :deprecated "0.5.2"
     :publics (concat-res
               [{:name 'added-fn
                 :arglists '([a b])
                 :type :var
                 :added "10.2.2"
                 :file "metagetta_test/test_ns1/special_tags.cljc"
                 :line 26}
                {:name 'deprecated-fn
                 :arglists '([x])
                 :type :var
                 :deprecated "0.4.0"
                 :file "metagetta_test/test_ns1/special_tags.cljc"
                 :line 10}]
               (when (not (in? opts :no-doc))
                 [{:name 'dont-doc-me,
                   :arglists '([x]),
                   :doc "no docs please\n",
                   :file "metagetta_test/test_ns1/special_tags.cljc",
                   :line 16,
                   :no-doc true,
                   :type :var}])
               (when (not (in? opts :skip-wiki))
                 [{:name 'dont-wiki-me,
                   :arglists '([y]),
                   :doc "no docs also please\n",
                   :file "metagetta_test/test_ns1/special_tags.cljc",
                   :line 30,
                   :skip-wiki true,
                   :type :var}])
               [{:name 'dynamic-def
                 :type :var
                 :doc "dynamic def docs\n"
                 :dynamic true
                 :file "metagetta_test/test_ns1/special_tags.cljc"
                 :line 21}])})))

(defn- analyze-sources
  "Analyze (by default all) sources from `test-sources`"
  [opts]
  (main/get-metadata (merge {:root-path "test-sources"} opts)))

(t/deftest analyze-cljs-code-test
  (let [actual (analyze-sources {:languages #{"cljs"}})
        expected {"cljs" (expected-result :cljs)}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-code-test
  (let [actual (analyze-sources {:languages #{"clj"}})
        expected {"clj" (expected-result :clj)}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-code-test
  (let [actual (analyze-sources {:languages #{"clj" "cljs"}})
        expected {"clj" (expected-result :clj)
                  "cljs" (expected-result :cljs)}]
    (t/is (= expected actual))))

(t/deftest analyze-clj-and-cljs-via-auto-detect-code-test
  (let [actual (analyze-sources {:languages :auto-detect})
        expected {"clj" (expected-result :clj)
                  "cljs" (expected-result :cljs)}]
    (t/is (= expected actual))))

(t/deftest ^:no-doc-test analyze-no-doc-test
  ;; this test is special in that it includes
  ;; namespaces marked with no-doc would fail if an attempt was made to load them
  ;; requires special setup, see test task in bb.edn
  (let [actual (analyze-sources {:root-path "target/no-doc-sources-test"
                                 :languages #{"clj" "cljs"}
                                 :exclude-with [:no-doc :skip-wiki]})
        expected {"clj" (expected-result :clj :no-doc :skip-wiki)
                  "cljs" (expected-result :cljs :no-doc :skip-wiki)}]
    (t/is (= expected actual))))

(t/deftest analyze-select-namespace-no-matches-test
  (let [actual (analyze-sources {:languages #{"clj" "cljs"}
                                 :namespaces ["wont.find.me"]})
        expected {"clj" []
                  "cljs" []}]
    (t/is (= expected actual))))

(t/deftest analyze-specify-namespaces-wildcard-test
  (let [actual (analyze-sources {:languages #{"clj" "cljs"}
                                 :namespaces ["metagetta-test.*"]})
        expected {"clj" (expected-result :clj)
                  "cljs" (expected-result :cljs)}]
    (t/is (= expected actual))))

(t/deftest analyze-specify-namespaces-subset-test
  (let [namespaces ['metagetta-test.cljs-macro-functions.usage
                    'metagetta-test.test-ns1.protocols]
        actual (analyze-sources {:languages #{"clj" "cljs"}
                                 :namespaces namespaces})
        expected {"clj" (filter #(in? namespaces (:name %)) (expected-result :clj))
                  "cljs" (filter #(in? namespaces (:name %)) (expected-result :cljs))}]
    (t/is (= expected actual))))
