;;
;; Note: this file is used in tests that rely on line numbers
;
(ns metagetta-test.test-ns1.no-doc-me-later
  "This namespace will be marked with no-doc at load-time"
  #?(:clj (:require [metagetta-test.util.alter-meta-clj :refer [alter-the-ns-meta-data!]])
     :cljs (:require-macros [metagetta-test.util.alter-meta-cljs :refer [alter-the-ns-meta-data!]])))

(defn some-var [x] x)

(alter-the-ns-meta-data! metagetta-test.test-ns1.no-doc-me-later
                         {:no-doc true})
