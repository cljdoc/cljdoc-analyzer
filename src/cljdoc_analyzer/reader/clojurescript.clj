(ns cljdoc-analyzer.reader.clojurescript
  "Read raw documentation information from ClojureScript source directory."
  (:require [clojure.java.io :as io]
            [cljs.analyzer.api :as ana]
            [cljs.closure]
            [cljs.env]
            [clojure.string :as str]
            [cljdoc-analyzer.reader.utils :as utils]))

(defn- cljs-filename? [filename]
  (or (.endsWith filename ".cljs")
      (.endsWith filename ".cljc")))

(defn- cljs-file? [file]
  (and (.isFile file)
       (-> file .getName cljs-filename?)))

(defn- remove-quote [x]
  (if (and (seq? x) (= (first x) 'quote))
    (second x)
    x))

(defn- strip-parent [parent]
  (let [len (inc (count (.getPath parent)))]
    (fn [child]
      (let [child-name (.getPath child)]
        (if (>= (count child-name) len)
          (io/file (subs child-name len)))))))

(defn- find-files [file]
  (if (.isDirectory file)
    (->> (file-seq file)
         (filter cljs-file?)
         (keep (strip-parent file)))))

(defn- no-doc? [var]
  (or (:skip-wiki var) (:no-doc var)))

(defn- protocol-methods [protocol vars]
  (let [proto-name (name (:name protocol))]
    (filter #(if-let [p (:protocol %)] (= proto-name (name p))) vars)))

(defn- multimethod? [var]
  (= (:tag var) 'cljs.core/MultiFn))

(defn- var-type [opts]
  (cond
   (:macro opts)           :macro
   (:protocol-symbol opts) :protocol
   (multimethod? opts)     :multimethod
   :else                   :var))

(defn- read-var [source-path file vars var]
  (let [vt (var-type var)
        normalize (partial utils/normalize-to-source-path source-path)]
    (-> var
        (select-keys [:name :file :line :arglists :doc :dynamic :added :deprecated :doc/format])
        (utils/update-some :name (comp symbol name))
        (utils/update-some :arglists remove-quote)
        (utils/update-some :doc utils/correct-indent)
        (utils/update-some :file normalize)
        (utils/assoc-some  :type    vt
                     :members (->> (protocol-methods var vars)
                                   (map (partial read-var source-path file vars))
                                   (map utils/remove-empties)
                                   (map #(dissoc % :file :line))
                                   (sort-by :name)))
        utils/remove-empties)))

(defn- unreferenced-protocol-fn?
  "Tools like potemkin import-vars can create a new function in one namespace point to an existing function within a protocol.
  In these cases, we want to include the new function."
  [source-path actual-file vars]
  (let [meta-file (utils/normalize-to-source-path source-path (:file vars))
        actual-file (utils/normalize-to-source-path source-path (str actual-file))]
    (and (:protocol vars) (= meta-file actual-file))))

(defn- read-publics [state namespace source-path file]
  (let [vars (vals (ana/ns-publics state namespace))
        unreferenced-protocol? (partial unreferenced-protocol-fn? source-path file)]
    (->> vars
         (remove :anonymous)
         (remove unreferenced-protocol?)
         (remove no-doc?)
         (map (partial read-var source-path file vars))
         (sort-by (comp str/lower-case :name)))))

(defn- analyze-file [file]
  (let [opts  (cljs.closure/add-implicit-options {})
        state (cljs.env/default-compiler-env opts)]
    (ana/no-warn
     (cljs.closure/validate-opts opts)
     (ana/analyze-file state file opts))
    state))

(defn- read-file [source-path file exception-handler]
  (try

    (let [source  (io/file source-path file)
          ns-name (:ns (ana/parse-ns source))
          state   (analyze-file source)]
      {ns-name
       (-> (ana/find-ns state ns-name)
           (select-keys [:name :doc])
           (utils/update-some :doc utils/correct-indent)
           (merge (-> ns-name meta (select-keys [:no-doc])))
           (utils/remove-empties)
           (assoc :publics (read-publics state ns-name source-path file)))})
    (catch Exception e
      (exception-handler e file))))

(defn read-namespaces
  "Read ClojureScript namespaces from a set of source directories
  (defaults to [\"src\"]), and return a list of maps suitable for
  documentation purposes.

  Supported options using the second argument:
    :exception-handler - function (fn [ex file]) to handle exceptions
    while reading a namespace

  The keys in the maps are:
    :name   - the name of the namespace
    :doc    - the doc-string on the namespace
    :author - the author of the namespace
    :publics
      :name       - the name of a public function, macro, or value
      :file       - the file the var was declared in
      :line       - the line at which the var was declared
      :arglists   - the arguments the function or macro takes
      :doc        - the doc-string of the var
      :type       - one of :macro, :protocol or :var
      :added      - the library version the var was added in
      :deprecated - the library version the var was deprecated in"
  ([] (read-namespaces ["src"] {}))
  ([paths] (read-namespaces paths {}))
  ([paths {:keys [exception-handler]
           :or {exception-handler (partial utils/default-exception-handler "ClojureScript")}}]
   (mapcat (fn [path]
             (let [path (io/file (utils/canonical-path path))
                   file-reader #(read-file path % exception-handler)]
               (->> (find-files path)
                    (map file-reader)
                    (apply merge)
                    (vals)
                    (remove :no-doc)
                    (sort-by :name))))
           paths)))
