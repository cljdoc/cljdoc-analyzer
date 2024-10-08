(ns ^:integration cljdoc-analyzer.main-shell-test
  "These tests do not represent the way cljdoc calls cljdoc-analyzer.
   They are here to ensure our command-line friendly adhoc interface works."
  (:require
   [babashka.fs :as fs]
   [cljdoc-analyzer.test-helper :as test-helper]
   [clojure.java.shell :as shell]
   [clojure.test :as t]
   [clojure.tools.deps :as tdeps]))

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
(t/deftest clj-base62
  ;; force re-download of artifact to local maven repo by deleting it first
  (let [project 'miikka/clj-base62
        version "0.1.1"
        {:keys [base path]} (tdeps/lib-location project {:mvn/version version} {})
        m2-repo-dir (fs/file base path)]
    (when (fs/exists? m2-repo-dir)
      (println "deleting" (str m2-repo-dir))
      (fs/delete-tree m2-repo-dir))
    (run-analysis (str project) version)))
