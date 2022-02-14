(ns ^:no-doc cljdoc-analyzer.file
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.nio.file Files)))

(defn copy [source target]
  (io/make-parents target)
  (with-open [in  (io/input-stream source)
              out (io/output-stream target)]
    (io/copy in out)))

;; move to babashka fs when available: https://github.com/babashka/fs/issues/31
(defn system-temp-file [prefix suffix]
  (.toFile (Files/createTempFile
            (clojure.string/replace prefix #"/" "-")
            suffix
            (into-array java.nio.file.attribute.FileAttribute []))))
