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

(defn- ns-matches? [{ns-name :name} pattern]
  (cond
    (instance? java.util.regex.Pattern pattern) (re-find pattern (str ns-name))
    (string? pattern) (= pattern (str ns-name))
    (symbol? pattern) (= pattern (symbol ns-name))))

(defn- filter-namespaces [namespaces ns-filters]
  (if (and ns-filters (not= ns-filters :all))
    (filter #(some (partial ns-matches? %) ns-filters) namespaces)
    namespaces))

(defn- read-namespaces
  [{:keys [language root-path namespaces] :as opts}]
  (let [record-constructor-function-vars #"^(map)?->\p{Upper}"
        reader (namespace-readers language)]
    (-> (reader root-path (select-keys opts [:exception-handler]))
        (filter-namespaces namespaces)
        (remove-excluded-vars record-constructor-function-vars))))

(defn- determine-languages [lang-opt src-dir]
  (if (= :auto-detect lang-opt)
    (utils/infer-platforms-from-src-dir (io/file src-dir))
    lang-opt))

(defn get-metadata
  "Get metadata from source files."
  ([{:keys [namespaces root-path languages]}]

   (assert (.exists (io/as-file root-path)))
   (assert (or (= :auto-detect languages)
               (and (set? languages) (>= (count languages) 1) (every? #{"clj" "cljs"} languages))))
   (let [root-path (utils/canonical-path root-path)
         actual-languages (determine-languages languages root-path)
         options {:namespaces (or namespaces :all)
                  :root-path root-path}]
     (->> (map #(do
                  (println "Analyzing for" %)
                  (read-namespaces (assoc options :language %)))
               actual-languages)
          (zipmap actual-languages)))))

(defn -main
  "This is intended to be called internally only to analyze clojure/clojurescript sources and return their metadata.

  Input is edn string of:
  - `:namespaces` - vector of namespaces to include or `:all` - defaults to `:all`
  - `:root-dir`- path to exploded/prepped jar dir
  - `:languages` - set of where language is `\"clj\"` and or `\"cljs\" or :auto-detect - defaults to `:auto-detect`
  - `:output-filename` - on success, edn is serialized to this file with special encoding for regexes.

  Launching a separate process for analysis is necessary to limit the dependencies to the minimum required.

  Caller is responsible for resolving and setting the classpath appropriately."
  [edn-arg]
  (try
    (let [{:keys [output-filename] :as args} (edn/read-string edn-arg)]
      (println "Args:" (-> (with-out-str (pprint/pprint args))
                           (string/trim)
                           (string/replace #"\n" "\n      ")))

      (println "Java version" (System/getProperty "java.version"))
      (println "Clojure version" (clojure-version))
      (println "ClojureScript version" (cljs-util/clojurescript-version))
      (->> (get-metadata args)
           (utils/serialize-cljdoc-edn)
           (spit output-filename))
      (println "Done"))

    (finally
      (flush))))
