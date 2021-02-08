(ns ^:no-doc cljdoc-analyzer.metagetta.utils
  "Miscellaneous utility functions."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn- empty-seq?[x]
  (and (seqable? x) (not (seq x))))

(defn remove-empties[m]
  (into {} (filter (comp not empty-seq? second) m)))

(defn assoc-some
  "Associates a key with a value in a map, if and only if the value is not nil."
  ([m k v]
     (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
     (reduce (fn [m [k v]] (assoc-some m k v))
             (assoc-some m k v)
             (partition 2 kvs))))

(defn update-some
  "Updates a key in a map with a function, if and only if the return value from
  the function is not nil."
  [m k f & args]
  (assoc-some m k (apply f (m k) args)))

(defn- find-minimum [coll]
  (when (seq coll)
    (apply min coll)))

(defn- find-smallest-indent [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (map #(re-find #"^\s+" %))
       (map count)
       (find-minimum)))

(defn canonical-path[file]
  (.getCanonicalPath (io/file file)))

(defn unindent
  "Unindent a block of text by a specific amount or the smallest common
  indentation size."
  ([text]
     (unindent text (find-smallest-indent text)))
  ([text indent-size]
     (let [re (re-pattern (str "^\\s{0," indent-size "}"))]
       (->> (str/split-lines text)
            (map #(str/replace % re ""))
            (str/join "\n")))))

(defn correct-indent [text]
  (when text
    (let [lines (str/split-lines text)]
      (->> (rest lines)
           (str/join "\n")
           (unindent)
           (str (first lines) "\n")))))

(defn uri-path [path]
  (str/replace (str path) java.io.File/separator "/"))

(defn normalize-path [path root]
  (when path
    (let [root (str (uri-path root) "/")
          path (uri-path (.getAbsolutePath (io/file path)))]
      (if (.startsWith path root)
        (.substring path (.length root))
        path))))

(defn- dejarify
  "Return the classpath relative file for `file`.

  Some manipulated metadata analysis :file elements (from techniques such as
  potemkin import-vars) can point to a file inside the jar instead of the file
  on file system when cljdoc-analyzer is invoked from cljdoc. This could likely be
  handled by altering the classpath cljdoc uses, but we handle the case
  here none-the-less.

  Oddly, the syntax of the file url is missing the jar: prefix so we add that
  if needed before converting."
  [file]
  (if (re-find #"^(jar:)?file:/.*\.jar!/" file)
    (->> (if (str/starts-with? file "jar:") file (str "jar:" file))
         java.net.URL.
         .openConnection
         (cast java.net.JarURLConnection)
         .getEntryName)
    file))

(defn normalize-to-source-path
  [source-path file]
  (when file
    (let [file (dejarify file)]
      (if (.exists (io/file file))
        (normalize-path file source-path)
        file))))

(defn infer-platforms-from-src-dir
  "Given a directory `src-dir` inspect all files and infer which
  platforms the source files likely target."
  [^java.io.File src-dir]
  (assert (< 1 (count (file-seq src-dir))) "jar contents dir does not contain any files")
  (let [file-types (->> (file-seq src-dir)
                        (keep (fn [f]
                                (cond
                                  (.endsWith (.getPath f) ".clj")  :clj
                                  (.endsWith (.getPath f) ".cljs") :cljs
                                  (.endsWith (.getPath f) ".cljc") :cljc))))]
    (case (set file-types)
      #{:clj}  ["clj"]
      #{:cljs} ["cljs"]
      ["clj" "cljs"])))

(defn default-exception-handler [lang e file]
  (throw (ex-info (format "Could not generate %s documentation for %s" lang file) {} e)))

(defn serialize-cljdoc-edn [analyze-result]
  ;; the analyzed structure can contain regex #"..." (e.g. in :arglists)
  ;; and they can't be read in again using edn/read-string
  ;; so there are changed to #regex"..." and read in with a custom reader
  (->> analyze-result
       (walk/postwalk #(if (instance? java.util.regex.Pattern %)
                         (tagged-literal 'regex (str %))
                         %))
       (pr-str)))

(defn new-failsafe-data-reader-fn
  "Return a new failsafe data reader that replaces unknown tagged literals with data via
  `clojure.core/tagged-literal` instead of failing the analysis."
  [ns-or-file]
  (let [warn-unknown-tagged-literal
        (fn [tag]
          (println "INFO [metagetta.utils] Beware: ns/file " ns-or-file " includes the unknown tagged literal `" tag "`, ignoring it and replacing the value with data. This should not influence the analysis unless the value is a top-level public var."))

        warn-unknown-tagged-literal-once
        (memoize warn-unknown-tagged-literal)]

    (fn failsafe-data-reader-fn [tag value]
      (warn-unknown-tagged-literal-once tag)
      (tagged-literal tag value))))
