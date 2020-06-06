(ns ^:no-doc cljdoc-analyzer.file
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.nio.file Files)))

(defn copy [source target]
  (io/make-parents target)
  (with-open [in  (io/input-stream source)
              out (io/output-stream target)]
    (io/copy in out)))

(defn delete-directory! [dir]
  (let [{:keys [files dirs]} (group-by (fn [f]
                                         (cond (.isDirectory f) :dirs
                                               (.isFile f) :files))
                                       (file-seq dir))]
    (doseq [f files] (.delete f))
    (doseq [d (reverse dirs)] (.delete d))))

(defn system-temp-dir [prefix]
  (.toFile (Files/createTempDirectory
            (clojure.string/replace prefix #"/" "-")
            (into-array java.nio.file.attribute.FileAttribute []))))

(defn system-temp-file [prefix suffix]
  (.toFile (Files/createTempFile
            (clojure.string/replace prefix #"/" "-")
            suffix
            (into-array java.nio.file.attribute.FileAttribute []))))
