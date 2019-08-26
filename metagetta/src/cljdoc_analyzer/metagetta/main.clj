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

(defn- add-var-defaults [vars defaults]
  (for [var vars]
    (-> (merge defaults var)
        (update-in [:members] add-var-defaults defaults))))

(defn- add-ns-defaults [namespaces defaults]
  (if (seq defaults)
    (for [namespace namespaces]
      (-> (merge defaults namespace)
          (update-in [:publics] add-var-defaults defaults)))
    namespaces))

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
  (let [exclude-vars #"^(map)?->\p{Upper}"
        metadata {}
        reader (namespace-readers language)]
    (-> (reader [root-path] (select-keys opts [:exception-handler]))
        (filter-namespaces namespaces)
        (remove-excluded-vars exclude-vars)
        (add-ns-defaults metadata))))

(def defaults
  ;; TODO: is this a good default for root-path?  I dunno we are working on exploded jars.
  (let [root-path (System/getProperty "user.dir")]
    {:language     :auto-detect
     :root-path    root-path
     :namespaces   :all}))

(defn determine-languages [lang-opt src-dir]
  (if (= :auto-detect lang-opt)
    (utils/infer-platforms-from-src-dir (io/file src-dir))
    lang-opt))

(defn get-metadata
  "Get metadata from source files."
  ([options]
   (let [options    (-> (merge defaults options)
                        (update :root-path utils/canonical-path))]
     (read-namespaces options))))

(defn -main
  "This is intended to be called internally only to analyze clojure/clojurescript
  sources and return their metadata.

  Launching a separate process for analysis is necessary to limit the
  dependencies to the minimum required.

  Caller is responsible for resolving and setting the classpath appropriately.

  One might argue that process isolation is not necessary for clojure vs
  clojurescript for a single project (we could do both runs in one process),
  but that's the way we'll roll for now."
  ;; TODO: if regexes are not by default serializable... so if any options contain regexes...
  [edn-arg]
  (try
    (let [{:keys [namespaces jar-contents-path languages output-filename] :as args} (edn/read-string edn-arg)
          actual-languages (determine-languages languages jar-contents-path)]
      ;; TODO: fixup languages validation
      #_(assert (#{"clj" "cljs"} language))
      (assert (.exists (io/as-file jar-contents-path)))
      (println "Args:" (-> (with-out-str (pprint/pprint args))
                           (string/trim)
                           (string/replace #"\n" "\n      ")))

      (println "Clojure version" (clojure-version))
      (println "ClojureScript version" (cljs-util/clojurescript-version))

      (->> (map #(do
                   (println "Analysing for" %)
                   (get-metadata {:namespaces namespaces
                                  :root-path jar-contents-path
                                  :language %}))
                actual-languages)
           (zipmap actual-languages)
           (utils/serialize-cljdoc-edn)
           (spit output-filename)))
    (finally
      (flush))))
