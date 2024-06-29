(ns ^:no-doc cljdoc-analyzer.cljdoc-main
  "Launch cljdoc-analyzer with arguments from edn string"
  (:require
   [cljdoc-analyzer.runner :as runner]
   [cljdoc-shared.analysis :as analysis]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]))

(defn -main
  [edn-arg]
  (let [{:keys [project version jarpath pompath extra-repos languages] :as args} (edn/read-string edn-arg)
        _  (log/info (str "args:\n" (with-out-str (pp/pprint args))))
        {:keys [analysis-status]} (runner/analyze! {:project project
                                                    :version version
                                                    :jarpath jarpath
                                                    :pompath pompath
                                                    :extra-repos extra-repos
                                                    :languages languages
                                                    :namespaces :all
                                                    :exclude-with [:no-doc :skip-wiki :mranderson/inlined]
                                                    :output-filename (analysis/result-path project version)})]
    (shutdown-agents)
    (System/exit (if (= :success analysis-status) 0 1))))
