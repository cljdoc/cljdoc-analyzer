(ns ^:no-doc cljdoc-analyzer.metagetta.utils
  "Miscellaneous utility functions."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk])
  (:import [java.time Duration]))

(defn- empty-coll?[x]
  (and (coll? x) (not (seq x))))

(defn remove-empties[m]
  (into {} (filter (comp not empty-coll? second) m)))

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
  (when (and text (not (str/blank? text)))
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
    (->> (if (.startsWith file "jar:") file (str "jar:" file))
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
  (let [src-dir (.getAbsoluteFile src-dir)
        file-types (->> (file-seq src-dir)
                        (filter #(.isFile %))
                        (map #(.relativize (.toPath src-dir) (.toPath %)))
                        (map str)
                        (remove #(.startsWith % "META-INF"))
                        (reduce (fn [acc f]
                                  (if-let [[_ ext] (re-find #".*\.(clj|cljs|cljc)$" f)]
                                    (conj acc ext)
                                    acc))
                                #{}))]
    (case file-types
      #{} (throw (ex-info "no Clojure/Clojurescript sources found" {}))
      #{"clj"}  ["clj"]
      #{"cljs"} ["cljs"]
      ["clj" "cljs"])))

(defn default-exception-handler [lang e file]
  (throw (ex-info (format "Could not generate %s documentation for %s" lang file) {} e)))

;; WARNING: this little fn is currently duped in cljdoc-shared/analysis-edn/serialize
;; We currently dupe instead of bringing the shared dep to minimize
;; metagetta deps...
(defn serialize-cljdoc-analysis-edn [analyze-result]
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
      ;(tagged-literal tag value) ; <-- OK for reading but breaks the Clojure Compiler's `emitValue` unless we define print-dup
      [:cljdoc/unknown-tagged-literal (name tag) value])))

(defn humanize-duration [^Duration duration]
  (-> duration
      str
      (subs 2)
      (str/replace #"(\d[HMS])" "$1 ")
      (str/lower-case)
      str/trim))

(defmacro time-op
  "Evaluates expr and prints the time it took.  Returns the value of expr."
  {:added "1.0"}
  [desc expr]
  `(let [start# (System/currentTimeMillis)
         ret# ~expr
         end# (System/currentTimeMillis)]
     (printf "⏱  %s took: %s\n" ~desc (humanize-duration (Duration/ofMillis (- end# start#))))
     ret#))


(defn parse-ns-name-with-meta
  "Return namespace name adorned with its metadata from unevaluated `ns-decl` form."
  [ns-decl]
  (when ns-decl
    (let [ns-name (second ns-decl)
          attr-map (->> ns-decl
                        (drop 2)
                        (take 2)
                        (filter map?)
                        first)]
      (vary-meta ns-name merge attr-map))))

(defn- contains-any-key? [m ks]
  (seq (select-keys m ks)))

(defn remove-analyzed-with-meta [exclude-with namespaces]
  (if exclude-with
    (->> (remove #(contains-any-key? % exclude-with) namespaces)
         (map (fn [ns]
                (update ns :publics
                        (fn [vars]
                          (remove #(contains-any-key? % exclude-with) vars))))))
    namespaces))
