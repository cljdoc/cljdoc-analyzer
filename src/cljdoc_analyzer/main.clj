(ns cljdoc-analyzer.main
  "Main namespace for generating documentation"
  (:require [clojure.pprint]
            [clojure.walk]
            [cljdoc-analyzer.reader.clojure :as clj]
            [cljdoc-analyzer.reader.clojurescript :as cljs]
            [cljdoc-analyzer.utils :as utils]))


(def ^:private namespace-readers
  {:clojure       clj/read-namespaces
   :clojurescript cljs/read-namespaces})

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
        source-paths [root-path]
        reader (namespace-readers language)]
    (-> (reader source-paths (select-keys opts [:exception-handler]))
        (filter-namespaces namespaces)
        (remove-excluded-vars exclude-vars)
        (utils/add-source-paths root-path source-paths)
        (add-ns-defaults metadata))))

(def defaults
  (let [root-path (System/getProperty "user.dir")]
    {:language     :clojure
     :root-path    root-path
     :namespaces   :all}))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([options]
   (let [options    (-> (merge defaults options)
                        (update :root-path utils/canonical-path)
                        (update :source-paths #(map utils/canonical-path %)))
         namespaces (read-namespaces options)]
     (assoc options :namespaces namespaces))))

(defn -main
  "The main entry point for reading API information from files in a directory.

  To analyze a project (debugging etc.) follow these steps:

  1. unzip the project's jar into a directory
  2. add the project's coordinates to the local `deps.edn` file
  3. add the jar contents directory to `:paths` in `deps.edn`

  You can then call this main function as follows:

      clj -m cljdoc-analyzer.main clojurescript jar-contents-dir/
      clj -m cljdoc-analyzer.main clojure jar-contents-dir/"
  [lang path]
  (println "Analyzing lang:" lang)
  (println "Analyzing path:" path)
  (assert (#{"clojure" "clojurescript"} lang))
  (->> (generate-docs {:root-path path
                       :language (keyword lang)})
       :namespaces
       ;; Walk/realize entire structure, otherwise "Excluding ->Xyz"
       ;; messages will be mixed with the pretty printed output
       (clojure.walk/prewalk identity)
       clojure.pprint/pprint))
