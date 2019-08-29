(ns cljdoc-analyzer.util
  "Utility functions :)

  These are available in the analysis environment and thus should work
  without any additional dependencies or further assumptions about
  what's on the classpath."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :as walk])
  (:import (java.nio.file Files Paths)))

(defn group-id [project]
  (or (if (symbol? project)
        (namespace project)
        (namespace (symbol project)))
      (name project)))

(defn artifact-id [project]
  (name (symbol project)))

(def analysis-output-prefix
  "The -main of `cljdoc-analyzer.runner` will write files to this directory.

  Be careful when changing it since that path is also hardcoded in the
  [cljdoc-builder](https://github.com/martinklepsch/cljdoc-builder)
  CircleCI configuration"
  "/tmp/cljdoc/analysis-out/")

(defn cljdoc-edn
  [project version]
  {:pre [(some? project) (string? version)]}
  (str "cljdoc-edn/" (group-id project) "/" (artifact-id project) "/" version "/cljdoc.edn"))

(defn serialize-cljdoc-edn [analyze-result]
  ;; the analyzed structure can contain regex #"..." (e.g. in :arglists)
  ;; and they can't be read in again using edn/read-string
  ;; so there are changed to #regex"..." and read in with a custom reader
  (->> analyze-result
       (walk/postwalk #(if (instance? java.util.regex.Pattern %)
                         (tagged-literal 'regex (str %))
                         %))
       (pr-str)))

(defn deserialize-cljdoc-edn [s]
  (edn/read-string {:readers {'regex re-pattern}} s))

(defn read-cljdoc-edn
  [file]
  {:pre [(some? file)]}
  (deserialize-cljdoc-edn (slurp file)))

(defn clojars-id [{:keys [group-id artifact-id]}]
  (if (= group-id artifact-id)
    artifact-id
    (str group-id "/" artifact-id)))

(defn copy [source file]
  (io/make-parents file)
  (with-open [in  (io/input-stream source)
              out (io/output-stream file)]
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
