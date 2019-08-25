(ns cljdoc-analyzer.main
  "Launch cljdoc-analyzer with arguments from edn string"
  (:require [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [cljdoc-analyzer.runner :as runner]))

(defn -main
  "Analyze the provided project"
  [edn-arg]
  (let [{:keys [project version jarpath pompath] :as args} (edn/read-string edn-arg)
        _                         (pp/pprint args)
        {:keys [analysis-status]} (runner/analyze! {:project project
                                                    :version version
                                                    :jarpath jarpath
                                                    :pompath pompath})]
    (shutdown-agents)
    (System/exit (if (= :success analysis-status) 0 1))))
