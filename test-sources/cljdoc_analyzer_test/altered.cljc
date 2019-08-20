;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns cljdoc-analyzer-test.altered
  (:require cljdoc-analyzer-test.protocols)
  #?(:clj (:require [cljdoc-analyzer-test.alter-meta-clj :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])
     :cljs (:require-macros [cljdoc-analyzer-test.alter-meta-cljs :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])))


(defmacro altered-macro-with-root-relative-file[]
  '(println "it's good day to lie"))
(alter-the-meta-data! cljdoc-analyzer-test.altered altered-macro-with-root-relative-file
                      {:doc "added doc"
                       :file "test-sources/cljdoc_analyzer_test/multimethod.cljc"
                       :line 3})



(defn altered-fn-with-source-relative-file[]
  '(prinln "I lie too"))
(alter-the-meta-data! cljdoc-analyzer-test.altered altered-fn-with-source-relative-file
                      {:file "cljdoc_analyzer_test/multimethod.cljc"
                       :line 14})


(def altered-def-with-absolute-file 42)
(alter-the-meta-data-abs! cljdoc-analyzer-test.altered altered-def-with-absolute-file
                      {:file "test-sources/cljdoc_analyzer_test/record.cljc"
                       :line 7})

(def fn-pointing-to-protocol-fn cljdoc-analyzer-test.protocols/operation-one)
(copy-the-meta-data! cljdoc-analyzer-test.altered fn-pointing-to-protocol-fn cljdoc-analyzer-test.protocols/operation-one)
