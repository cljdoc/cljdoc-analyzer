(ns ^:no-doc cljdoc-analyzer.metagetta.clojure
  "Read raw documentation information from Clojure source directory."
  (:import java.util.jar.JarFile
           java.io.FileNotFoundException)
  (:require [clojure.java.io :as io]
            [cljdoc-analyzer.metagetta.inlined.toolsnamespace.v1v4v0.clojure.tools.namespace.find :as ns-find]
            [cljdoc-analyzer.metagetta.utils :as utils]))

(defn try-require [namespace]
  (try
    (require namespace)
    (catch FileNotFoundException _ nil)))

(defn core-typed? []
  (find-ns 'clojure.core.typed.check))

(defn var->symbol [var]
  (let [{:keys [ns name]} (meta var)]
    (symbol (str ns) (str name))))

(defn typecheck-namespace [namespace]
  ((find-var 'clojure.core.typed/check-ns-info) namespace))

(defn typecheck-var [var]
  ((find-var 'clojure.core.typed/check-form-info) (var->symbol var)))

(defn- public-vars [namespace]
  (->> (ns-publics namespace)
       (vals)))

(defn- proxy? [var]
  (re-find #"proxy\$" (-> var meta :name str)))

(defn- macro? [var]
  (:macro (meta var)))

(defn- multimethod? [var]
  (instance? clojure.lang.MultiFn (var-get var)))

(defn- protocol? [var]
  (let [value (var-get var)]
    (and (map? value)
         (not (sorted? value)) ; workaround for CLJ-1242
         (:on-interface value))))

(defn- protocol-method? [vars var]
  (when-let [p (:protocol (meta var))]
    (some #{p} vars)))

(defn- protocol-methods [protocol vars]
  (filter #(= protocol (:protocol (meta %))) vars))

(defn- var-type [var]
  (cond
   (macro? var)       :macro
   (multimethod? var) :multimethod
   (protocol? var)    :protocol
   :else              :var))

(defn core-typed-type [var]
  (let [{:keys [delayed-errors ret]} (typecheck-var var)]
    (when (empty? delayed-errors)
      (:t ret))))

(defn- read-var [source-path vars var]
  (let [normalize (partial utils/normalize-to-source-path source-path)
        {:keys [doc] :as metadata} (meta var)]
    (-> metadata
        (select-keys [:name :file :line :arglists :dynamic
                      :added :deprecated
                      :no-doc :skip-wiki :mranderson/inlined])
        (utils/assoc-some :doc (utils/correct-indent doc))
        (utils/update-some :file normalize)
        (utils/assoc-some  :type (var-type var)
                     :type-sig (when (core-typed?) (core-typed-type var))
                     :members (seq (map (partial read-var source-path vars)
                                        (protocol-methods var vars))))
        utils/remove-empties)))

(defn- read-publics [source-path namespace]
  (let [vars (public-vars namespace)]
    (->> vars
         (remove proxy?)
         (remove (partial protocol-method? vars))
         (map (partial read-var source-path vars)))))

(defn- read-ns [namespace source-path exception-handler]
  (try-require 'clojure.core.typed.check)
  (when (core-typed?)
    (typecheck-namespace namespace))
  (try
    (binding [*default-data-reader-fn* (utils/new-failsafe-data-reader-fn namespace)]
      (require namespace))
    (let [{:keys [doc] :as metadata} (-> namespace find-ns meta)]
         (-> metadata
             (select-keys [:author :deprecated :added :no-doc :skip-wiki :mranderson/inlined])
             (assoc :name namespace)
             (assoc :publics (read-publics source-path namespace))
             (utils/assoc-some :doc (utils/correct-indent doc))
             (list)))
    (catch Exception e
      (exception-handler e namespace))))

(defn- jar-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".jar"))))

(defn- find-namespaces
  "Return found namespaces in dir or jar `file`.
   Each namespace will be adorned with any static metadata specified on ns name or via attr-map. "
  [file]
  (let [ns-decls (cond
                   (.isDirectory file) (ns-find/find-ns-decls-in-dir file)
                   (jar-file? file)    (ns-find/find-ns-decls-in-jarfile (JarFile. file)))]
    (map utils/parse-ns-name-with-meta ns-decls)))

(defn remove-with-meta [exclude-with coll]
  (if exclude-with
    (remove #(-> % meta (select-keys exclude-with) seq)
            coll)
    coll))

(defn read-namespaces
  "Read Clojure namespaces from a source directory and return a list
   namespaces with their public vars.

  Supported options using the second argument:
    :exception-handler - function (fn [ex ns]) to handle exceptions
                         while reading a namespace
    :exclude-with - coll of metadata keywords to exclude, applied
                    first to namespaces, then to vars

  The keys in the maps are:
    :name      - the name of the namespace
    :doc       - the doc-string on the namespace
    :author    - if the metadata is there, we return it
    :no-doc    - request for namespace not to be documented
    :skip-wiki - legacy synonym for :no-doc
    :mranderson/inlined - default metadata for mranderson inline namespace
    :publics
      :name       - the name of a public function, macro, or value
      :file       - the file the var was declared in
      :line       - the line at which the var was declared
      :arglists   - the arguments the function or macro takes
      :doc        - the doc-string of the var
      :type       - one of :macro, :protocol, :multimethod or :var
      :added      - the library version the var was added in
      :deprecated - the library version the var was deprecated in
      :no-doc     - request for var not to be documented
      :skip-wiki    - legacy synonym for :no-doc
      :mranderson/inlined - default meta for mranderson inlined"

  ([path] (read-namespaces path {}))
  ([path {:keys [exception-handler exclude-with]
           :or {exception-handler (partial utils/default-exception-handler "Clojure")}}]
   (let [path (utils/canonical-path path)]
     (->> (io/file path)
          (find-namespaces)
          ;; shot at excluding namespaces before analysis/load
          (remove-with-meta exclude-with)
          (mapcat #(read-ns % path exception-handler))
          ;; final exclude of namespaces and vars after analysis
          (utils/remove-analyzed-with-meta exclude-with)))))
