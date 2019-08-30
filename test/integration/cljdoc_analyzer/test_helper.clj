(ns cljdoc-analyzer.test-helper
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cljdoc-analyzer.util :as util]))

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
  (t/is (= (util/read-cljdoc-edn (io/resource (edn-filename "expected-edn" project version)))
           (util/read-cljdoc-edn edn-out-filename))))
