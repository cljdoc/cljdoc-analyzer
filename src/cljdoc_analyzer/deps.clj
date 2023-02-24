(ns ^:no-doc cljdoc-analyzer.deps
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.tools.deps :as tdeps]
            [clojure.string :as string]
            [version-clj.core :as v]
            [cljdoc-shared.pom :as pom]
            [cljdoc-shared.proj :as proj]))

(defn- ensure-recent-ish [deps-map]
  (let [min-versions {'org.clojure/clojure "1.9.0"
                      'org.clojure/clojurescript "1.10.773"
                      'org.clojure/java.classpath "0.2.2"
                      'org.clojure/core.async "0.4.474"}
        choose-version (fn choose-version [given-v min-v]
                         (if (pos? (v/version-compare min-v given-v)) min-v given-v))]
    (reduce (fn [dm [proj min-v]]
              (cond-> dm
                (get dm proj) (update-in [proj :mvn/version] choose-version min-v)))
            deps-map
            min-versions)))

(defn- ensure-required-deps [deps-map]
  (merge {'org.clojure/clojure {:mvn/version "1.11.1"}
          'org.clojure/clojurescript {:mvn/version "1.11.60"}
          ;; many ring libraries implicitly depend on this and getting all
          ;; downstream libraries to properly declare it as a "provided"
          ;; dependency would be a major effort. since it's all java it also
          ;; shouldn't affect Clojure-related dependency resolution
          'javax.servlet/javax.servlet-api {:mvn/version "4.0.1"}}
         deps-map))

(defn cljdoc-analyzer-metagetta-dep!
  "Returns appropriate dependency for metagetta. If we are running from a jar we'll find the embedded metagetta.jar as a resource, extract that `target-dir` and reference it. Otherwise we'll simply point to metagetta in our source tree."
  [target-dir]
  {'cljdoc-analyzer/metagetta
   {:local/root (if-let [metagetta-jar-resource (io/resource "metagetta.jar")]
                  (let [target-jar (str (fs/file target-dir "metagetta.jar"))]
                    (fs/copy-tree metagetta-jar-resource target-jar)
                    target-jar)
                  (io/resource "metagetta"))}})

(defn- extra-pom-deps
  "Some projects require additional depenencies that have either been specified with
  scope 'provided', 'system', 'test', are marked 'optional' or are specified via documentation, e.g. a README.
  Maybe should be able to configure this via their git repo user cljdoc.edn configuration
  file but this situation being an edge case this is a sufficient fix for now."
  [pom]
  (->> (:dependencies pom)
       ;; compile/runtime scopes will be included by the normal dependency resolution.
       (filter #(or (#{"provided" "system" "test"} (:scope %))
                    (:optional %)))
       ;; The version can be nil when pom's utilize
       ;; dependencyManagement this unsurprisingly breaks tools.deps
       ;; Remains to be seen if this causes any issues
       ;; http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management
       (remove #(nil? (:version %)))
       (remove #(string/starts-with? (:artifact-id %) "boot-"))
       ;; Ensure that tools.reader version is used as specified by CLJS
       (remove #(and (= (:group-id %) "org.clojure")
                     (= (:artifact-id %) "tools.reader")))
       (map (fn [{:keys [group-id artifact-id version]}]
               [(symbol group-id artifact-id) {:mvn/version version}]))
       (into {})))

(defn- clj-cljs-deps
  [pom]
  (->> (:dependencies pom)
       (map (fn [{:keys [group-id artifact-id version]}]
              [(symbol group-id artifact-id) {:mvn/version version}]))
       (filter #(-> % first #{'org.clojure/clojure 'org.clojure/clojurescript}))
       (into {})))

(defn- pom-repos
  [pom]
  (->> (:repositories pom)
       (map (fn [repo] [(:id repo) (dissoc repo :id)]))
       (into {})))

(defn- deps
  "Create a deps.edn style :deps map for the project specified by the
  Jsoup document `pom`."
  [pom metagetta-dep compensating-deps]
  (-> (extra-pom-deps pom)
      (merge (clj-cljs-deps pom))
      (merge compensating-deps)
      (ensure-required-deps)
      (ensure-recent-ish)
      (merge metagetta-dep)))

(defn resolved-deps
  "Returns resolved deps for `pom-url` and include `jar-url` as one of those deps.
  Include `compensating-deps` when needed."
  [work-dir jar-url pom-url default-repos extra-repos compensating-deps]
  {:pre [(string? jar-url) (string? pom-url)]}
  (let [pom (pom/parse (slurp pom-url))
        project (proj/clojars-id (:artifact-info pom))
        metagetta-dep (cljdoc-analyzer-metagetta-dep! work-dir)
        repos (merge default-repos (pom-repos pom) extra-repos)]
    (tdeps/resolve-deps {:deps (deps pom metagetta-dep compensating-deps)
                         :mvn/repos repos}
                        {:extra-deps {(symbol project) {:local/root jar-url}}
                         :verbose false})))

(defn resolve-dep
  "Return resolved local `:jar` and `:pom` for maven repo hosted `project` `version`"
  [project version default-repos extra-repos]
  (let [jar (-> (tdeps/resolve-deps {:deps {project {:mvn/version version}}
                                     :mvn/repos (merge default-repos extra-repos)} nil)
                (get project)
                :paths
                first)
        pom (string/replace jar #"\.jar$" ".pom")]
    {:jar jar :pom pom}))

(defn make-classpath
  "Build a classpath for `resolved-deps`."
  [resolved-deps]
  (-> (tdeps/make-classpath-map {:paths []} resolved-deps nil)
      :classpath-roots
      tdeps/join-classpath))

(defn print-tree [resolved-deps]
  (tdeps/print-tree resolved-deps))
