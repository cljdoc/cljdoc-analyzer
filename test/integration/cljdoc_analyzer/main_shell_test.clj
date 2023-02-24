(ns ^:integration cljdoc-analyzer.main-shell-test
  "These tests do not represent the way cljdoc calls cljdoc-analyzer.
   They are here to ensure our command-line friendly adhoc interface works."
  (:require [babashka.fs :as fs]
            [clojure.test :as t]
            [clojure.java.shell :as shell]
            [cljdoc-analyzer.test-helper :as test-helper]))

(defn- run-analysis [project version]
  (println "Analyzing" project version)
  (let [edn-out-filename (str (fs/create-temp-file {:prefix "cljdoc-analysis-edn"
                                                    :suffix ".edn"}))]
    (fs/delete-on-exit edn-out-filename)
    (test-helper/verify-analysis-result project version edn-out-filename
                                        (shell/sh "clojure" "-M" "--report" "stderr" "-m" "cljdoc-analyzer.main"
                                                  "analyze"
                                                  "--project" project
                                                  "--version" version
                                                  "--exclude-with" ":no-doc"
                                                  "--exclude-with" ":skip-wiki"
                                                  "--exclude-with" ":mranderson/inlined"
                                                  "--output-filename" edn-out-filename))))

;; main testing is done in cljdoc-main-test, this is a sanity run that this main path works as well.
(t/deftest bidi
  (run-analysis "bidi" "2.1.3"))
