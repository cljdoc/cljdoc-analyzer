;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns ^{:mranderson/inlined true} cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.dir
  (:require [cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.file :as file]
            [cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.find :as find]
            [cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.track :as track]
            [cljdoc-analyzer.metagetta.inlined.javaclasspath.v1v0v0.clojure.java.classpath :refer [classpath-directories]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.io File) (java.util.regex Pattern)))

(defn- find-files [dirs platform]
  (->> dirs
       (map io/file)
       (map #(.getCanonicalFile ^File %))
       (filter #(.exists ^File %))
       (mapcat #(find/find-sources-in-dir % platform))
       (map #(.getCanonicalFile ^File %))))

(defn- modified-files [tracker files]
  (filter #(< (::time tracker 0) (.lastModified ^File %)) files))

(defn- deleted-files [tracker files]
  (set/difference (::files tracker #{}) (set files)))

(defn- update-files [tracker deleted modified {:keys [read-opts]}]
  (let [now (System/currentTimeMillis)]
    (-> tracker
        (update-in [::files] #(if % (apply disj % deleted) #{}))
        (file/remove-files deleted)
        (update-in [::files] into modified)
        (file/add-files modified read-opts)
        (assoc ::time now))))

(defn scan-files
  "Scans files to find those which have changed since the last time
  'scan-files' was run; updates the dependency tracker with
  new/changed/deleted files.

  files is the collection of files to scan.

  Optional third argument is map of options:

    :platform  Either clj (default) or cljs, both defined in
               cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.find, controls reader options for 
               parsing files.

    :add-all?  If true, assumes all extant files are modified regardless
               of filesystem timestamps."
  {:added "0.3.0"}
  ([tracker files] (scan-files tracker files nil))
  ([tracker files {:keys [platform add-all?]}]
   (let [deleted (seq (deleted-files tracker files))
         modified (if add-all?
                    files
                    (seq (modified-files tracker files)))]
     (if (or deleted modified)
       (update-files tracker deleted modified platform)
       tracker))))

(defn scan-dirs
  "Scans directories for files which have changed since the last time
  'scan-dirs' or 'scan-files' was run; updates the dependency tracker
  with new/changed/deleted files.

  dirs is the collection of directories to scan, defaults to all
  directories on Clojure's classpath.

  Optional third argument is map of options:

    :platform  Either clj (default) or cljs, both defined in 
               cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.find, controls file extensions 
               and reader options.

    :add-all?  If true, assumes all extant files are modified regardless
               of filesystem timestamps."
  {:added "0.3.0"}
  ([tracker] (scan-dirs tracker nil nil))
  ([tracker dirs] (scan-dirs tracker dirs nil))
  ([tracker dirs {:keys [platform add-all?] :as options}]
   (let [ds (or (seq dirs) (classpath-directories))]
     (scan-files tracker (find-files ds platform) options))))

(defn scan
  "DEPRECATED: replaced by scan-dirs.

  Scans directories for Clojure (.clj, .cljc) source files which have
  changed since the last time 'scan' was run; update the dependency
  tracker with new/changed/deleted files.

  If no dirs given, defaults to all directories on the classpath."
  {:added "0.2.0"
   :deprecated "0.3.0"}
  [tracker & dirs]
  (scan-dirs tracker dirs {:platform find/clj}))

(defn scan-all
  "DEPRECATED: replaced by scan-dirs.

  Scans directories for all Clojure source files and updates the
  dependency tracker to reload files. If no dirs given, defaults to
  all directories on the classpath."
  {:added "0.2.0"
   :deprecated "0.3.0"}
  [tracker & dirs]
  (scan-dirs tracker dirs {:platform find/clj :add-all? true}))
