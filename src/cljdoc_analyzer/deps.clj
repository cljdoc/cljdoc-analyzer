(ns ^:no-doc cljdoc-analyzer.deps
  (:require
   [babashka.fs :as fs]
   [cljdoc-shared.proj :as proj]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.deps :as tdeps]
   [version-clj.core :as v]
   ;; the following not part of the tools deps public API, will reconsider if this causes us any grief
   [clojure.tools.deps.extensions :as tdeps-ext]
   [clojure.tools.deps.extensions.pom :as tdeps-pom]
   [clojure.tools.deps.util.maven :as tdeps-maven]
   [clojure.tools.deps.util.session :as tdeps-session])
  (:import
   [org.apache.maven.model Dependency Model Repository]
   [org.apache.maven.model.building StringModelSource]))

(set! *warn-on-reflection* true)

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

(defn- model-dep->data
  "Taken from clojure/tools.deps `clojure.tools.deps.extensions.pom` and modified to not bother with gathering exclusions."
  [^Dependency dep]
  (let [scope (.getScope dep)
        optional (.isOptional dep)
        artifact-id (.getArtifactId dep)
        classifier (.getClassifier dep)]
    [(symbol (.getGroupId dep) (if (string/blank? classifier) artifact-id (str artifact-id "$" classifier)))
     (cond-> {:mvn/version (.getVersion dep)}
       scope (assoc :scope scope)
       optional (assoc :optional true))]))

(defn non-transitive-deps
  [^Model pom]
  (->> (.getDependencies pom)
       (map model-dep->data)))

(defn provided-deps
  "Returns a deps map of provided, system, test scoped and optional deps from `deps-vec`"
  [deps-vec]
  (->> deps-vec
       (filter (fn [[_project coord]]
                 (or (#{"provided" "system" "test"} (:scope coord))
                     (:optional coord))))
       ;; remove any problematic provided deps
       (remove (fn [[project _coord]] (= 'org.clojure/tools.reader project)))
       (remove (fn [[project _coord]] (string/starts-with? (name project) "boot-")))
       (into {})))

(defn- clj-cljs-deps
  "Returns a deps map of clojure and/or clojurescript deps from `deps-vec`"
  [deps-vec]
  (->> deps-vec
       (filter (fn [[project _coord]]
                 (-> project  #{'org.clojure/clojure 'org.clojure/clojurescript})))
       (into {})))

(defn- pom-repos
  "Returns maven repositories declared with `pom`"
  [^Model pom]
  (->> (.getRepositories pom)
       (map (fn [^Repository repo] [(.getId repo) {:url (.getUrl repo)}]))
       (into {})))

(defn- overriding-deps
  "Returns a deps map from `pom` that is used as overrides for the actual deps in `pom`."
  [pom metagetta-dep compensating-deps]
  (let [non-transitive-deps (non-transitive-deps pom)] ;; this includes optional, provided, test, etc
    (->> (merge (provided-deps non-transitive-deps)
                (clj-cljs-deps non-transitive-deps))
         (reduce-kv (fn [m project coord]
                      (assoc m project (dissoc coord :optional :scope :exclusions)))
                    {})
         (merge compensating-deps)
         (ensure-required-deps)
         (ensure-recent-ish)
         (merge metagetta-dep))))

(defn read-pom-model
  "Taken from clojure/tools.deps `clojure.tools.deps.extensions.pom` and modified to support reading from a slurpable-pom."
  ^Model [slurpable-pom config]
  (let [pom-str (slurp slurpable-pom)
        settings (tdeps-session/retrieve :mvn/settings #(tdeps-maven/get-settings))]
    (tdeps-session/retrieve
      {:pom :model :source slurpable-pom}
      #(tdeps-pom/read-model (StringModelSource. pom-str) config settings))))

(defn resolved-deps
  "Returns resolved (and tweaked) deps for `pom-url`, includes `jar-url` as a dep.
  Include `compensating-deps` when needed."
  [work-dir jar-url pom-url default-repos extra-repos compensating-deps]
  {:pre [(string? jar-url) (string? pom-url)]}
  (let [repos (merge default-repos extra-repos)
        pom (read-pom-model pom-url {:mvn/repos repos})
        project (proj/clojars-id {:group-id (.getGroupId pom) :artifact-id (.getArtifactId pom)})
        metagetta-dep (cljdoc-analyzer-metagetta-dep! work-dir)
        repos (merge default-repos (pom-repos pom) extra-repos)]
    (tdeps/resolve-deps {:deps (overriding-deps pom metagetta-dep compensating-deps)
                         :mvn/repos repos}
                        {:extra-deps {(symbol project) {:local/root jar-url}}
                         :verbose false})))

(defn resolve-artifact
  "Return resolved (downloaded, if necessary) local `:jar` and `:pom` for maven repo
  hosted `project` `version`withour resolving project's dependencies."
  [project version default-repos extra-repos]
  (let [lib project
        repos (merge default-repos extra-repos)
        coord {:mvn/version version}
        ;; this seems to resolve and downlaod the the jar
        jar (first (tdeps-ext/coord-paths lib
                     coord
                     :mvn {:mvn/repos repos}))]
    ;; this seems to resolve and download the pom
    (tdeps-ext/coord-deps lib
                    coord
                    :mvn {:mvn/repos repos})
    {:jar jar :pom (string/replace jar #"\.jar$" ".pom")}))

(defn make-classpath
  "Build a classpath for `resolved-deps`."
  [resolved-deps]
  (-> (tdeps/make-classpath-map {:paths []} resolved-deps nil)
      :classpath-roots
      tdeps/join-classpath))

(defn print-tree [resolved-deps]
  (tdeps/print-tree resolved-deps))
