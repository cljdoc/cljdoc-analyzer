;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns cljdoc-analyzer-test.metagetta.altered
  (:require cljdoc-analyzer-test.metagetta.protocols)
  #?(:clj (:require [cljdoc-analyzer-test.util.alter-meta-clj :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])
     :cljs (:require-macros [cljdoc-analyzer-test.util.alter-meta-cljs :refer [alter-the-meta-data! alter-the-meta-data-abs! copy-the-meta-data!]])))


(defmacro altered-macro-with-root-relative-file[]
  '(println "it's good day to lie"))
(alter-the-meta-data! cljdoc-analyzer-test.metagetta.altered altered-macro-with-root-relative-file
                      {:doc "added doc"
                       :file "test-sources/cljdoc_analyzer_test/metagetta/multimethod.cljc"
                       :line 3})



(defn altered-fn-with-source-relative-file[]
  '(prinln "I lie too"))
(alter-the-meta-data! cljdoc-analyzer-test.metagetta.altered altered-fn-with-source-relative-file
                      {:file "cljdoc_analyzer_test/metagetta/multimethod.cljc"
                       :line 14})


(def altered-def-with-absolute-file 42)
(alter-the-meta-data-abs! cljdoc-analyzer-test.metagetta.altered altered-def-with-absolute-file
                      {:file "test-sources/cljdoc_analyzer_test/metagetta/record.cljc"
                       :line 7})

(def fn-pointing-to-protocol-fn cljdoc-analyzer-test.metagetta.protocols/operation-one)
(copy-the-meta-data! cljdoc-analyzer-test.metagetta.altered fn-pointing-to-protocol-fn cljdoc-analyzer-test.metagetta.protocols/operation-one)
