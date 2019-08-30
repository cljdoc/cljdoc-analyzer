(ns ^:integration cljdoc-analyzer.main-shell-test
  (:require [clojure.test :as t]
            [clojure.java.shell :as shell]
            [cljdoc-analyzer.test-helper :as test-helper]))

(defn- temp-edn-filename []
  (str
   (doto (java.io.File/createTempFile "cljdoc-edn" ".edn")
     (.deleteOnExit)
     (.getAbsolutePath))))

(defn- run-analysis [project version]
  (println "Analyzing" project version)
  (let [edn-out-filename (temp-edn-filename)]
    (test-helper/verify-analysis-result project version edn-out-filename
                                        (shell/sh "clojure" "--report" "stderr" "-m" "cljdoc-analyzer.main"
                                                  "analyze"
                                                  "--project" project
                                                  "--version" version
                                                  "--output-filename" edn-out-filename))))

;; main testing is done in cljdoc-main-test, this is a sanity run that this main path works as well.
(t/deftest orchestra
  (run-analysis "orchestra" "2018.11.07-1"))

(t/deftest bidi
  (run-analysis "bidi" "2.1.3"))
