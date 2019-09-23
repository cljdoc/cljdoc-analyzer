(ns cljdoc-analyzer.test-helper
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cljdoc-analyzer.analysis-edn :as analysis-edn]))

(defn edn-filename [prefix project version]
  (let [project (if (string/index-of project "/")
                  project
                  (str project "/" project))]
    (str prefix "/" project "/" version "/cljdoc.edn")))


(defn verify-analysis-result [ project version edn-out-filename {:keys [exit out err]} ]
  (println "analysis exit code:" exit)
  (println "analysis stdout:")
  (println out)
  (println "analysis stderr:")
  (println err)
  (t/is (zero? exit))
  (let [expected-f (io/resource (edn-filename "expected-edn" project version))]
    (when-not expected-f
      (throw (ex-info "expected edn file missing"
                      {:project project
                       :version version
                       :path (edn-filename "expected-edn" project version)})))
    (t/is (= (analysis-edn/read-cljdoc-edn expected-f)
             (analysis-edn/read-cljdoc-edn edn-out-filename)))))
