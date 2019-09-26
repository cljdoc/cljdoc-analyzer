(ns ^:no-doc cljdoc-analyzer.cljdoc-main
  "Launch cljdoc-analyzer with arguments from edn string"
  (:require [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [cljdoc-analyzer.proj :as proj]
            [cljdoc-analyzer.runner :as runner]))

(defn cljdoc-edn-filepath
  [project version]
  {:pre [(some? project) (string? version)]}
  (str "cljdoc-edn/" (proj/group-id project) "/" (proj/artifact-id project) "/" version "/cljdoc.edn"))

(def analysis-output-prefix
  "The -main of `cljdoc-analyzer.runner` will write files to this directory.

  Be careful when changing it since that path is also hardcoded in the
  [cljdoc-builder](https://github.com/martinklepsch/cljdoc-builder)
  CircleCI configuration"
  "/tmp/cljdoc/analysis-out/")

(defn -main
  [edn-arg]
  (let [{:keys [project version jarpath pompath extra-repos] :as args} (edn/read-string edn-arg)
        _  (log/info (str "args:\n" (with-out-str (pp/pprint args))))
        {:keys [analysis-status]} (runner/analyze! {:project project
                                                    :version version
                                                    :jarpath jarpath
                                                    :pompath pompath
                                                    :extra-repos extra-repos
                                                    :namespaces :all
                                                    :exclude-with [:no-doc :skip-wiki]
                                                    :output-filename (str analysis-output-prefix (cljdoc-edn-filepath project version))})]
    (shutdown-agents)
    (System/exit (if (= :success analysis-status) 0 1))))
