(ns ^:no-doc cljdoc-analyzer.file
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.nio.file Files Paths FileSystems SimpleFileVisitor FileVisitResult Paths CopyOption)
           (java.util Collections)))

(defn copy [source target]
  (io/make-parents target)
  (with-open [in  (io/input-stream source)
              out (io/output-stream target)]
    (io/copy in out)))

(defn copy-resource
  "Recursively copy resource `resource-name` to `target-dir`.

  options:
  - :path-patterns - copy only files with paths matching regex patterns in this vector. Defaults to copying all files.

  Special handling included for copying resources found in jar.

  Our use case is recursively copying metagetta src and deps.edn to a temp dir. Other uses could require tweaks."
  ([resource-name target-dir]
   (copy-resource resource-name target-dir {}))
  ([resource-name target-dir options]
   (let [{:keys [path-patterns] :or {path-patterns [#".*"]}} options
         res-url (io/resource resource-name)
         proto (.getProtocol res-url)
         res-uri (.toURI res-url)
         path (if (= "jar" proto)
                (.getPath (FileSystems/newFileSystem res-uri (Collections/emptyMap))
                          (str "/" resource-name) (into-array String []))
                (Paths/get res-uri))
         target-path (Paths/get target-dir (into-array String []))]
     (Files/walkFileTree path (proxy [SimpleFileVisitor] []
                                (preVisitDirectory [dir attrs]
                                  ;; we don't know if we need to create the dir until we check if the file matches
                                  ;; our :path-patterns
                                  FileVisitResult/CONTINUE)
                                (visitFile [file attrs]
                                  (when (some #(re-matches % (str file)) path-patterns)
                                    (let [target (.resolve target-path (str (.relativize path file)))]
                                      (io/make-parents (.toFile target))
                                      (Files/copy file target (into-array CopyOption []))))
                                  FileVisitResult/CONTINUE))))))

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
