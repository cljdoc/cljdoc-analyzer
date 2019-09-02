(ns cljdoc-analyzer.main
  (:require [cli-matic.core :as cli]
            [cljdoc-analyzer.deps :as deps]
            [cljdoc-analyzer.runner :as runner]))

(defn analyze [{:keys [project version] :as args}]
  (let [{:keys [jar pom]} (deps/resolve-dep (symbol project) version)]
    (runner/analyze! (-> (select-keys args [:project :version :exclude-with :output-filename])
                         (assoc :jarpath jar :pompath pom)))))

(def cli-config
  {:app {:command "cljdoc-analyzer"
         :description "Returns namespaces and their publics"
         :version "0.0.1"}

   :commands
   [{:command     "analyze"
     :description ["Get namespaces and publics for your project."]
     :opts        [{:option "project" :short "p"
                    :as "Project to import"
                    :type :string :default :present}

                   {:option "version" :short "v"
                    :as "Project version to import"
                    :type :string :default :present}

                   {:option "exclude-with" :short "e"
                    :as "Exclude namespaces and publics with metadata key present"
                    :type :keyword :multiple true}

                   {:option "output-filename" :short "o"
                    :as "Where to write edn output"
                    :type :string :default :present}]
     :runs        analyze}]})

(defn -main[& args]
  (cli/run-cmd args cli-config)
  (shutdown-agents))
