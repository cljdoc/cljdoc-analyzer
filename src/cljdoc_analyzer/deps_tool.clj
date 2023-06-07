(ns ^:no-doc cljdoc-analyzer.deps-tool
  "Entry point for running as a Clojure CLI tool"
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [cljdoc-analyzer.deps :as deps]
            [cljdoc-analyzer.runner :as runner]
            [cljdoc-analyzer.config :as config]))

(defn- extra-repo-arg-to-option [extra-repos]
  (reduce (fn [acc n]
            (let [[id url] (string/split n #" ")]
              (assoc acc id {:url url})))
    {}
    extra-repos))

(defn maybe-download [{:keys [download extra-repos project version]}]
  (when download
    (deps/resolve-artifact
      (symbol project) version
      (:repos (config/load))
      (extra-repo-arg-to-option extra-repos))))

(defn analyze
  [{:keys [project version jarpath pompath extra-repos output-filename] :as args}]
  (let [_  (log/info (str "args:\n" (with-out-str (pp/pprint args))))
        {:keys [jar pom]} (maybe-download args)
        {:keys [analysis-status]} (runner/analyze! 
                                    (merge
                                      (maybe-download args)
                                      {:project project
                                       :version version
                                       :jarpath (or jarpath jar)
                                       :pompath (or pompath pom)
                                       :extra-repos extra-repos
                                       :namespaces :all
                                       :exclude-with [:no-doc :skip-wiki :mranderson/inlined]
                                       :output-filename (or output-filename
                                                          (str "output-" project "-" version ".edn"))}))]
    (shutdown-agents)
    (System/exit (if (= :success analysis-status) 0 1))))

(defn analyze-local [_]
  (let [jars (fs/list-dir "target" "*.jar")
        jar (-> jars first str)]
    (assert (= 1 (count jars)) "Expected to find exactly 1 target/*.jar file")
    (analyze {:project (System/getProperty "user.dir")
              :version "snapshot"
              :pompath "pom.xml"
              :jarpath jar})))
