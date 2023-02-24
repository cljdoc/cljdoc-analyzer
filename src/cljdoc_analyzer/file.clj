(ns ^:no-doc cljdoc-analyzer.file
  (:require [clojure.java.io :as io]))

(defn copy [source target]
  (io/make-parents target)
  (with-open [in  (io/input-stream source)
              out (io/output-stream target)]
    (io/copy in out)))

