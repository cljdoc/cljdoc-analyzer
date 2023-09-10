(ns ^:no-doc cljdoc-analyzer.metagetta.main
  "Main namespace for generating documentation"
  (:require [clojure.walk :as walk]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [cljdoc-analyzer.metagetta.clojure :as clj]
            [cljdoc-analyzer.metagetta.utils :as utils]))

(defn- namespace-readers [lang]
  (case lang
    "clj" clj/read-namespaces
    ;; don't bring in clourescript support unless necessary.
    ;; this allows us to analyze older versions of clojure which are not
    ;; compatible with current versions of clojurescript
    "cljs" (do
             (require '[cljdoc-analyzer.metagetta.clojurescript :as cljs])
             (resolve 'cljs/read-namespaces))))

(defn- var-symbol [namespace var]
  (symbol (name (:name namespace)) (name (:name var))))

(defn- map-factory-for
  "Return true if var meta looks like a defrecord generated map factory function."
  [{var-name :name doc :doc}]
  (when (and var-name (re-find #"^map->\p{Upper}" (name var-name))
             doc (re-matches #"Factory function for (class |)\S+, taking a map of keywords to field values\.\R?" doc))
    {:factory-fn :map
     :factory-from (-> var-name str (subs 5) symbol)}))

(defn- positional-factory-for
  "Return true if var meta looks like a defrecord/deftype generated positonal factory function."
  [{var-name :name doc :doc}]
  (when (and var-name (re-find #"^->\p{Upper}" (name var-name))
             doc (re-matches #"Positional factory function for (class |)\S+\.\R?" doc))
    {:factory-fn :positional
     :factory-from (-> var-name str (subs 2) symbol)}))

(defn- adorn-factories [vars]
  (map #(merge % (or (map-factory-for %)
                     (positional-factory-for %)))
       vars))

(defn- adorn-factory-targets [vars]
  (let [fndx (->> vars
                  (filter :factory-fn)
                  (group-by :factory-from))]
    (if (seq fndx)
      (->> vars
           (map (fn [{var-name :name :as var-meta}]
                  (if-let [fs (get fndx var-name nil)]
                    (assoc var-meta :factory-fns fs)
                    var-meta))))
      vars)))

(defn- exclude-vars
  "Remove defrecord, deftype and any generated defrecord/deftype factory fns.

  Code is currently generic for Clojure and ClojureScript, note:
  - Clojure does not currently include defrecord and deftype (but does include associated factory fns)
  but this code assumes defrecord and deftype might be there.
  - ClojureScript does include entries for defrecrod and deftype. It does seem to mark records with :record,
  so we could get more clever/specific for ClojureScript if we wanted, but I'm not sure if this would be
  relying more on internals.
  - All adorned vars are currently removed, if they were included we'd have to dissoc some special identifying keywords."
  [vars namespace]
  (->> vars
       adorn-factories
       adorn-factory-targets
       (remove (fn [var-meta]
                 (when (or (:factory-fn var-meta) (:factory-fns var-meta))
                   (println "Excluding" (var-symbol namespace var-meta))
                   true)))))

(defn- exclude-unwanted-vars [namespaces]
  (map #(update-in % [:publics] exclude-vars %) namespaces))

(defn- assert-no-dupes-in-publics [namespaces]
  (let [dupes (for [ns namespaces
                    :let [dupes (->> (:publics ns)
                                     (group-by :name)
                                     (remove #(= 1 (count (val %)))))]
                    :when (seq dupes)]
                {:namespace (:name ns) :dupes dupes})]
    (if (seq dupes)
      (throw (ex-info (str "duplicate publics found:\n" (with-out-str (pprint/pprint dupes))) {:dupes dupes}))
      namespaces)))

(defn- case-insensitive-comparator [a b]
  (let [la (and a (string/lower-case a))
        lb (and b (string/lower-case b))]
     (if (= la lb)
       (compare a b)
       (compare la lb))))

(defn- sort-by-name
  "Sorts all collections of maps with :name by :name.
  Sort is case insensitive with consistent sort order if :name is case sensitive unique across collection
  and all maps in collection have :name."
  [namespaces]
  (walk/postwalk #(if (and (coll? %) (:name (first %)))
                            (sort-by :name case-insensitive-comparator %)
                            %)
                         namespaces))

(defn- read-namespaces
  [{:keys [language root-path] :as opts}]
  (let [reader (namespace-readers language)]
    (-> (reader root-path (select-keys opts [:exception-handler :exclude-with :namespaces]))
        exclude-unwanted-vars
        (assert-no-dupes-in-publics)
        (sort-by-name))))

(defn- determine-languages [lang-opt src-dir]
  (->> (if (= :auto-detect lang-opt)
         (utils/infer-platforms-from-src-dir (io/file src-dir))
         lang-opt)
       (into [])
       distinct
       sort))

(defn get-metadata
  "Get metadata from source files."
  ([{:keys [namespaces root-path languages exclude-with]}]
   (assert (or (= :all namespaces)
               (every? #(or (string? %) (symbol? %)) namespaces))
           ":namespaces must be either :all or a vector of symbols (absolute match) and/or strings (regex match)")
   (assert (.exists (io/as-file root-path))
           ":root-path must exist")
   (assert (or (= :auto-detect languages)
               (and (coll? languages) (>= (count languages) 1) (every? #{"clj" "cljs"} languages)))
           ":languages must be either :auto-detect or a collection of one or both of: \"clj\", \"cljs\"")
   (let [root-path (utils/canonical-path root-path)
         actual-languages (determine-languages languages root-path)]
     ;; trigger early load of needed analysis support, loading lazily later
     ;; causes zprint (and maybe other libs that use sci) to fail cljs analysis
     (run! namespace-readers actual-languages)
     (->> actual-languages
          (mapv (fn [lang]
                  (println "Analyzing for" lang)
                  (utils/time-op (str "Analysis for " lang)
                                 (read-namespaces {:namespaces namespaces
                                                   :root-path root-path
                                                   :language lang
                                                   :exclude-with exclude-with}))))
          (zipmap actual-languages)))))

(defn -main
  "This is intended to be called internally only. It relies on the caller doing proper setup.

  Input is edn string of:
  - `:root-dir`- path to exploded/prepped jar dir
  - `:languages` - set of where language is `\"clj\"` and or `\"cljs\" or :auto-detect - defaults to `:auto-detect`
  - `:namespaces` - vector of namespaces to include or `:all` - defaults to `:all`
    when namespace is a symbol, match is absolute
    when namespace is a string, match is by regular expression
  - `:output-filename` - on success, edn is serialized to this file with special encoding for regexes.
  - `:exclude-with` - optional - exclude ns and publics with any key in vector - ex [:no-doc :skip-wiki :mranderson/inlined]

  Launching a separate process for analysis is necessary to limit the dependencies to the minimum required.

  Caller is responsible for resolving and setting the classpath appropriately."
  [edn-arg]
  (try
    (let [{:keys [namespaces output-filename] :as args} (edn/read-string edn-arg)]
      (println "Args:" (-> (with-out-str (pprint/pprint args))
                           (string/trim)
                           (string/replace #"\n" "\n      ")))

      (println "Java version" (System/getProperty "java.version"))
      (->> (get-metadata (assoc args :namespaces (or namespaces :all)))
           (utils/serialize-cljdoc-analysis-edn)
           (spit output-filename))
      (println "Done"))

    (finally
      (flush)
      (shutdown-agents))))
