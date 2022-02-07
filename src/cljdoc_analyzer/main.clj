(ns ^:no-doc cljdoc-analyzer.main
  (:require [cli-matic.core :as cli]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [cljdoc-analyzer.config :as config]
            [cljdoc-analyzer.deps :as deps]
            [cljdoc-analyzer.runner :as runner]))

(defn- extra-repo-arg-to-option [extra-repo]
  (reduce (fn [acc n]
            (let [[id url] (string/split n #" ")]
              (assoc acc id {:url url})))
          {}
          extra-repo))

(defn analyze [{:keys [project version extra-repo] :as args}]
  (let [config (config/load)
        extra-repos (extra-repo-arg-to-option extra-repo)
        {:keys [jar pom]} (deps/resolve-dep (symbol project) version (:repos config) extra-repos)]
    (runner/analyze! (-> (merge 
                           {:exclude-with [:no-doc :skip-wiki]} 
                           (select-keys args [:project :version :exclude-with :output-filename]))
                         (assoc :jarpath jar :pompath pom :extra-repos extra-repos)))))

(spec/def ::extra-repo
  (fn [vals] (every? #(= 2 (count (string/split % #" "))) vals)))

(expound/defmsg ::extra-repo "each extra-repo must be a quoted 'id url' pair")

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

                   {:option "extra-repo" :short "r"
                    :as "Include extra maven repo using quoted syntax 'repo-id repo-url' repeat for multiple"
                    :spec ::extra-repo
                    :type :string :multiple true}

                   {:option "output-filename" :short "o"
                    :as "Where to write edn output"
                    :type :string :default :present}]
     :runs        analyze}]})

(defn -main[& args]
  (cli/run-cmd args cli-config)
  (shutdown-agents))
