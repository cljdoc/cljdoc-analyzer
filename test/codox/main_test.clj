(ns codox.main-test
  (:require [clojure.test :as t]
            [codox.main :as main]))

(defn- analyze-sources [language]
  (->> (main/generate-docs {:source-paths ["test-sources"]
                            :root-path "test-sources"
                            :language language
                            :writer 'clojure.core/identity})
       :namespaces))

(defn- check-paths [ns-data]
  (update ns-data :publics
          #(map (fn [v]
                  ;; cljdoc does not currently use the :path, but it might change its mind so we check it
                  (t/is (= (str (:path v)) (:file v)) "for our tests config, :file and :path should be the equivalent")
                  (dissoc v :path))
                %)))

(defn- publics [r ns]
  (some->> r
           (filter #(= (:name %) ns))
           first
           check-paths))

(defn- common-analysis-testing [analysis]

  (t/testing "altered-metadata"
    (t/is (= {:name 'codox-test.altered
              :publics [{:name 'altered-def-with-absolute-file
                         :type :var
                         :file "codox_test/record.cljc"
                         :line 7}
                        {:name 'altered-fn-with-source-relative-file
                         :arglists '([])
                         :type :var
                         :file "codox_test/multimethod.cljc"
                         :line 14}
                        {:name 'altered-macro-with-root-relative-file
                         :arglists '([])
                         :type :macro
                         :doc "added doc\n"
                         :file "codox_test/multimethod.cljc"
                         :line 3}
                        {:arglists '([x]),
                         :doc "Operation 1 docs\n",
                         :file "codox_test/protocols.cljc",
                         :line 6,
                         :name 'fn-pointing-to-protocol-fn,
                         :type :var}]}
             (publics analysis 'codox-test.altered))))

  (t/testing "macros"
    (t/is (= {:name 'codox-test.macro
              :publics [{:name 'macdoc
                         :arglists '([a b c d])
                         :type :macro
                         :doc "Macro docs\n"
                         :file "codox_test/macro.cljc"
                         :line 23}
                        {:name 'simple
                         :arglists '([a b])
                         :type :macro
                         :file "codox_test/macro.cljc"
                         :line 11}
                        {:name 'varargs
                         :arglists '([a & xs])
                         :type :macro
                         :file "codox_test/macro.cljc"
                         :line 17}]}
             (publics analysis 'codox-test.macro))))

  (t/testing "multiarity"
    (t/is (= {:name 'codox-test.multiarity
              :publics [{:name 'multiarity
                         :arglists '([] [a] [a b] [a b c d])
                         :type :var
                         :doc "Multiarity comment\n"
                         :file "codox_test/multiarity.cljc"
                         :line 7}]}
             (publics analysis 'codox-test.multiarity))))

  (t/testing "multimethods"
    (t/is (= {:name 'codox-test.multimethod
              :publics [{:name 'start
                         :type :multimethod
                         :file "codox_test/multimethod.cljc"
                         :line 6}]}
             (publics analysis 'codox-test.multimethod))))

  (t/testing "no-doc-ns"
    (t/is (= [] (filter #(= (:name %) 'codox-test.no-doc-ns) analysis))))

  (t/testing "protocols"
    (t/is (= {:name 'codox-test.protocols
              :publics [{:name 'ProtoTest
                         :type :protocol
                         :doc "Protocol comment.\n"
                         :members '({:arglists ([a]), :name alpha :type :var}
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
                         :file "codox_test/protocols.cljc"
                         :line 6}]}
             (publics analysis 'codox-test.protocols))))


  (t/testing "records"
    (t/is (= {:name 'codox-test.record
              :publics [{:name 'DefRecordTest
                         :type :var
                         :file "codox_test/record.cljc"
                         :line 6}
                        {:name 'record-test
                         :type :var
                         :file "codox_test/record.cljc"
                         :line 8}]}
             (publics analysis 'codox-test.record))))

  (t/testing "special-tags"
    (t/is (= {:name 'codox-test.special-tags
              :publics [{:name 'added-fn
                         :arglists '([a b])
                         :type :var
                         :added "10.2.2"
                         :file "codox_test/special_tags.cljc"
                         :line 26}
                        {:name 'deprecated-fn
                         :arglists '([x])
                         :type :var
                         :deprecated "0.4.0"
                         :file "codox_test/special_tags.cljc"
                         :line 10}
                        {:name 'dynamic-def
                         :type :var
                         :doc "dynamic def docs\n"
                         :dynamic true
                         :file "codox_test/special_tags.cljc"
                         :line 21}]}
             (publics analysis 'codox-test.special-tags)))))

(t/deftest analyze-clojure-code-test
  (let [a (analyze-sources :clojure)]
    (common-analysis-testing a)))

(t/deftest analyze-clojurecript-code-test
  (let [a (analyze-sources :clojurescript)]
    (common-analysis-testing a)))
