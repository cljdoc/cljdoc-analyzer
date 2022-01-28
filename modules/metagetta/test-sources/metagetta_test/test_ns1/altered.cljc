;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.altered
  (:require metagetta-test.test-ns1.protocols)
  #?(:clj (:require [metagetta-test.util.alter-meta-clj :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])
     :cljs (:require-macros [metagetta-test.util.alter-meta-cljs :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])))


(defmacro altered-macro-with-root-relative-file[]
  '(println "it's good day to lie"))
(alter-the-meta-data! metagetta-test.test-ns1.altered/altered-macro-with-root-relative-file
                      {:doc "added doc"
                       :file "{metagetta.test.sources.root}/metagetta_test/test_ns1/multimethod.cljc"
                       :line 3})



(defn altered-fn-with-source-relative-file[]
  '(prinln "I lie too"))
(alter-the-meta-data! metagetta-test.test-ns1.altered/altered-fn-with-source-relative-file
                      {:file "metagetta_test/test_ns1/multimethod.cljc"
                       :line 14})


(def altered-def-with-absolute-file 42)
(alter-the-meta-data-abs! metagetta-test.test-ns1.altered/altered-def-with-absolute-file
                          {:file "{metagetta.test.sources.root}/metagetta_test/test_ns1/record.cljc"
                           :line 7})

(def fn-pointing-to-protocol-fn metagetta-test.test-ns1.protocols/operation-one)
(copy-the-meta-data! metagetta-test.test-ns1.altered/fn-pointing-to-protocol-fn metagetta-test.test-ns1.protocols/operation-one)

