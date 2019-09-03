(ns cljdoc-analyzer.metagetta.main
  "Main namespace for generating documentation"
  (:require [clojure.walk]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [cljs.util :as cljs-util]
            [cljdoc-analyzer.metagetta.clojure :as clj]
            [cljdoc-analyzer.metagetta.clojurescript :as cljs]
            [cljdoc-analyzer.metagetta.utils :as utils]))

(def ^:private namespace-readers
  {"clj"  clj/read-namespaces
   "cljs" cljs/read-namespaces})

(defn- var-symbol [namespace var]
  (symbol (name (:name namespace)) (name (:name var))))

(defn- remove-matching-vars [vars re namespace]
  (remove (fn [var]
            (when (and re (re-find re (name (:name var))))
              (println "Excluding var" (var-symbol namespace var))
              true))
          vars))

(defn- remove-excluded-vars [namespaces exclude-vars]
  (map #(update-in % [:publics] remove-matching-vars exclude-vars %) namespaces))

(defn- contains-any-key? [m ks]
  (seq (select-keys m ks)))

(defn- remove-excluded-keys [namespaces exclude-with]
  (->> (remove #(contains-any-key? % exclude-with) namespaces)
       (map (fn [ns]
              (update ns :publics
                      (fn [vars]
                        (remove #(contains-any-key? % exclude-with) vars)))))))

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
  (clojure.walk/postwalk #(if (and (coll? %) (:name (first %)))
                            (sort-by :name case-insensitive-comparator %)
                            %)
                         namespaces))

(defn- ns-matches? [{ns-name :name} pattern]
  (cond
    (string? pattern) (re-find (re-pattern pattern) (str ns-name))
    (symbol? pattern) (= pattern (symbol ns-name))))

(defn- filter-namespaces [namespaces ns-filters]
  (if (and ns-filters (not= ns-filters :all))
    (filter #(some (partial ns-matches? %) ns-filters) namespaces)
    namespaces))

(defn- read-namespaces
  [{:keys [language root-path namespaces exclude-with] :as opts}]
  (let [record-constructor-function-vars #"^(map)?->\p{Upper}"
        reader (namespace-readers language)]
    (-> (reader root-path (select-keys opts [:exception-handler]))
        (filter-namespaces namespaces)
        (remove-excluded-vars record-constructor-function-vars)
        (remove-excluded-keys exclude-with)
        (assert-no-dupes-in-publics)
        (sort-by-name))))

(defn- determine-languages [lang-opt src-dir]
  (if (= :auto-detect lang-opt)
    (utils/infer-platforms-from-src-dir (io/file src-dir))
    lang-opt))


(defn get-metadata
  "Get metadata from source files."
  ([{:keys [namespaces root-path languages exclude-with]}]
   (assert (or (= :all namespaces)
               (every? #(or (string? %) (symbol? %)) namespaces))
           ":namespaces must be either :all or a vector of symbols (absolute match) and/or strings (regex match)")
   (assert (.exists (io/as-file root-path))
           ":root-path must exist")
   (assert (or (= :auto-detect languages)
               (and (set? languages) (>= (count languages) 1) (every? #{"clj" "cljs"} languages)))
           ":languages must be either :auto-detect or a set of set of one or both of: \"clj\", \"cljs\"")
   (let [root-path (utils/canonical-path root-path)
         actual-languages (determine-languages languages root-path)]
     (->> (map (fn [lang]
                  (println "Analyzing for" lang)
                 (read-namespaces {:namespaces namespaces
                                    :root-path root-path
                                    :language lang
                                    :exclude-with exclude-with}))
               actual-languages)
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
  - `:exclude-with` - optional - exclude ns and publics with any key in vector - ex [:no-doc :skip-wiki]

  Launching a separate process for analysis is necessary to limit the dependencies to the minimum required.

  Caller is responsible for resolving and setting the classpath appropriately."
  [edn-arg]
  (try
    (let [{:keys [namespaces output-filename] :as args} (edn/read-string edn-arg)]
      (println "Args:" (-> (with-out-str (pprint/pprint args))
                           (string/trim)
                           (string/replace #"\n" "\n      ")))

      (println "Java version" (System/getProperty "java.version"))
      (println "Clojure version" (clojure-version))
      (println "ClojureScript version" (cljs-util/clojurescript-version))
      (->> (get-metadata (assoc args :namespaces (or namespaces :all)))
           (utils/serialize-cljdoc-edn)
           (spit output-filename))
      (println "Done"))

    (finally
      (flush))))
