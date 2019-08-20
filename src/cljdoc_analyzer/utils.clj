(ns cljdoc-analyzer.utils
  "Miscellaneous utility functions."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

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
  (if (seq coll)
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
  (if text
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


(defn default-exception-handler [lang e file]
  (println
   (format "Could not generate %s documentation for %s - root cause: %s %s"
           lang
           file
           (.getName (class e))
           (.getMessage e)))
  (.printStackTrace e))
