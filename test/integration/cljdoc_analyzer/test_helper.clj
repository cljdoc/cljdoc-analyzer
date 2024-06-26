(ns cljdoc-analyzer.test-helper
  (:require
   [babashka.fs]
   [cljdoc-shared.analysis-edn :as analysis-edn]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :as t]))

(defn edn-filename [prefix project version]
  (let [project (if (string/index-of project "/")
                  project
                  (str project "/" project))]
    (str prefix "/" project "/" version "/cljdoc-analysis.edn")))

(defn- filter-namespace
  "Filter undesired namespace in a analysis map.
  Useful when arguments of function are generated by gensyms: no reproducible analysis"
  [analysis-map language remove-ns?]
  (update-in analysis-map
             [:analysis language]
             ;; % is the list of namespaces
             #(remove (fn [ns] (remove-ns? (str (:name ns)))) %)))

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
    (let [expected-analysis (analysis-edn/read expected-f)
          actual-analysis (analysis-edn/read (str edn-out-filename))]
      (cond
        ;; For specter package, filter the "com.rpl.specter.impl" namespace
        (and (= project "com.rpl/specter")
             (= version "1.1.3"))
        (t/is (= (-> expected-analysis
                     (filter-namespace "clj" #{"com.rpl.specter.impl"})
                     (filter-namespace "cljs" #{"com.rpl.specter.impl"}))
                 (-> actual-analysis
                     (filter-namespace "clj" #{"com.rpl.specter.impl"})
                     (filter-namespace "cljs" #{"com.rpl.specter.impl"}))))

        :else
        (t/is (= expected-analysis
                 actual-analysis))))))
