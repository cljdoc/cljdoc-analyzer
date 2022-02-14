(ns cljdoc-analyzer.runner
  "Prepares the environment then launches the internal metagetta subproject for analysis.

  Constructs a directory with the sources of the analyzed jar present as well as
  some additional files that contain the actual code used during analysis. That
  code is than run by shelling out to `java` providing all deps via
  `-cp`.

  By shelling out a separate process we create an isolated environment which
  does not have the dependencies of cljdoc-analyzer."
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pprint]
            [cljdoc-analyzer.config :as config]
            [cljdoc-analyzer.deps :as deps]
            [cljdoc-analyzer.file :as file]
            [cljdoc-shared.proj :as proj]
            [cljdoc-shared.spec.analyzer :as analyzer-spec]
            [cljdoc-shared.analysis-edn :as analysis-edn])
  (:import (java.util.zip ZipFile)
           (java.net URI)))

(defn- download-jar! [jar-uri target-dir]
  (let [jar-f (io/file target-dir "downloaded.jar")]
    (log/infof "Downloading %s" jar-uri)
    (file/copy jar-uri jar-f)
    (.getPath jar-f)))

(defn- unzip!
  [source target-dir]
  (with-open [zip (ZipFile. (io/file source))]
    (let [entries (enumeration-seq (.entries zip))]
      (doseq [entry entries
              :when (not (.isDirectory ^java.util.zip.ZipEntry entry))
              :let [f (io/file target-dir (str entry))]]
        (file/copy (.getInputStream zip entry) f)))))

(defn- clean-jar-contents!
  "Some projects include their `out` directories in their jars,
  usually somewhere under public/, this tries to clear those.

  It also deletes various files that frequently trip up analysis.

  NOTE this means projects with the group-id `public` will fail to build."
  [unpacked-jar-dir]
  (when (.exists (io/file unpacked-jar-dir "public"))
    (log/info "Deleting public/ dir")
    (fs/delete-tree (io/file unpacked-jar-dir "public")))
  ;; Delete the clj-kondo exports directory, which includes clj files
  ;; with namespaces that don't match their directory location.
  ;; This fixes the issue https://github.com/cljdoc/cljdoc/issues/455
  (when (.exists (io/file unpacked-jar-dir "clj-kondo.exports"))
    (log/info "Deleting clj-kondo.exports/ dir")
    (fs/delete-tree (io/file unpacked-jar-dir "clj-kondo.exports")))
  ;; Delete .class files that have a corresponding .clj or .cljc file
  ;; to circle around https://dev.clojure.org/jira/browse/CLJ-130
  ;; This only affects Jars with AOT compiled namespaces where the
  ;; version of Clojure used during compilation is < 1.8.
  ;; This hast mostly been put into place for datascript and might
  ;; get deleted if datascript changes it's packaging strategy.
  (doseq [class-file (->> (file-seq unpacked-jar-dir)
                          (map #(.getAbsolutePath %))
                          (filter (fn clj-or-cljc [path]
                                    (or (.endsWith path ".cljc")
                                        (.endsWith path ".clj"))))
                          (map #(string/replace % #"(\.clj$|\.cljc$)" "__init.class"))
                          (map io/file))]
    (when (.exists class-file)
      (log/info "Deleting" (.getPath class-file))
      (.delete class-file)))
  (doseq [path ["deps.cljs" "data_readers.clj" "data_readers.cljc"]
          :let [file (io/file unpacked-jar-dir path)]]
    ;; TODO: is this still relevant now that we have switched to metagetta?
    ;; codox returns {:publics ()} for deps.cljs, data_readers.cljc
    ;; when present this should probably be fixed in codox as well
    ;; but just deleting the file will also do the job for now
    (when (.exists file)
      (log/info "Deleting" path)
      (.delete file))))

(defn- resolve-jar!
  "Returns local path to `jar`, if download necessary, downloads to `target-dir`"
  [jar target-dir]
  (let [jar-uri (URI. jar)]
    (if (boolean (.getHost jar-uri))
      (download-jar! jar-uri target-dir)
      jar)))

(defn- unpack-jar!
  "Returns dir where jar contents has been unpacked under `target-dir`.  dir
  will be relative to target-dir in a way suitable for use with a pom.xml as a
  local root."
  [local-jar-path target-dir]
  ;; If a pom is read from target-dir, src/main/clojure relative to that pom is
  ;; added to the classpath by tdeps.
  (let [jar-contents-dir (io/file target-dir "src/main/clojure")]
    (unzip! local-jar-path jar-contents-dir)
    (clean-jar-contents! jar-contents-dir)
    jar-contents-dir))

(defn- log-process-result [proc]
  (log/info (str "metagetta results:\nexit-code " (:exit proc)
                 "\nstdout:\n " (-> proc
                                    :out
                                    (string/trim)
                                    (string/replace #"\n" "\n "))
                 (when (seq (:err proc))
                   (str "\nstderr:\n " (-> proc
                                           :err
                                           (string/trim)
                                           (string/replace #"\n" "\n ")))))))

(defn- log-dependencies [resolved-deps]
  (log/info (str "dependencies for analysis:\n"
                 (with-out-str (deps/print-tree resolved-deps)))))

(defn- log-overrides [overrides]
  (when overrides
    (log/info (str "the following project overrides are active:\n"
                   (with-out-str (pprint/pprint overrides))))))

(defn- launch-metagetta
  "Analysis to get metadata is launched in a separate process to minimize dependencies to those of project being analyzed."
  [{:keys [project namespaces src-dir languages exclude-with classpath]}]
  (let [metadata-output-file (file/system-temp-file project ".edn")]
    (log/info "launching metagetta for:" project "languages:" languages)
    (let [analysis-args {:namespaces namespaces
                         :root-path (str src-dir)
                         :languages languages
                         :output-filename  (.getAbsolutePath metadata-output-file)
                         :exclude-with exclude-with}
          process (sh/sh "java"
                         "-cp" classpath
                         "-Dclojure.spec.skip-macros=true"
                         "-Dclojure.main.report=stderr"
                         "clojure.main"
                         "-m" "cljdoc-analyzer.metagetta.main"
                         (pr-str analysis-args)
                         ;; supplying :dir is necessary to avoid local deps.edn being included
                         ;; once -Srepro is finalized it might be useful for this purpose
                         :dir (.getParentFile metadata-output-file))
          _ (log-process-result process)]
      (if (zero? (:exit process))
        (let [result (analysis-edn/read metadata-output-file)]
          (assert result "No data was saved in output file")
          result)
        (throw (ex-info (str "Analysis failed with code " (:exit process))
                        {:code (:exit process)
                         :stdout (:out process)
                         :stderr (:err process)}))))))

(defn- validate-result [ana-result]
  (analyzer-spec/assert-result-full ana-result)
  (if (every? some? (vals ana-result))
    ana-result
    (throw (Exception. "Analysis failed"))))

(defn- save-result [ana-result output-file]
  (doto output-file
    (io/make-parents)
    (analysis-edn/write ana-result)))

(defn- get-metadata*
  "Return metadata for project"
  [{:keys [project version jarpath pompath default-repos extra-repos overrides] :as opts}]
  {:pre [(seq project) (seq version) (seq jarpath) (seq pompath)]}
  (let [work-dir (-> {:prefix (str "cljdoc-" (string/escape project {\/ \-}) "-" version)}
                     fs/create-temp-dir
                     fs/file)]
    (try
      (let [project (symbol project)
            local-jar-path (resolve-jar! jarpath work-dir)
            jar-contents-dir (unpack-jar! local-jar-path work-dir)
            pom-str (slurp pompath)
            ;; Utilize unpack-jar!'s deps.edn support, and create a path which
            ;; tdeps can understand.
            _ (spit (io/file work-dir "pom.xml") pom-str)
            resolved-deps (deps/resolved-deps work-dir
                                              (str work-dir) pompath
                                              default-repos extra-repos
                                              (:deps overrides))
            classpath (deps/make-classpath resolved-deps)]
        (log-overrides overrides)
        (log-dependencies resolved-deps)
        (-> {:group-id (proj/group-id project)
             :artifact-id (proj/artifact-id project)
             :version version
             :analysis (launch-metagetta (assoc opts
                                                :src-dir (.getPath jar-contents-dir)
                                                :languages (or (:languages overrides) :auto-detect)
                                                :namespaces (or (:namespaces overrides) :all)
                                                :classpath classpath))
             :pom-str pom-str}
            (validate-result)))
      (finally
        (fs/delete-tree work-dir)))))

(defn get-metadata
  "Return analysis result map for:
  - `:project` - project artifact-id/group-id
  - `:version` - project version
  - `:jarpath` - path to jar file
  - `:pompath` - path to pom file
  - `:extra-repos` - optional map of additional maven repos.

  To serialize/deserialize result see [[cljdoc-analyzer.analysis-edn]]."
  [{:keys [project] :as opts}]
  (let [config (config/load)
        project (proj/normalize project)
        overrides (get-in config [:project-overrides project])]
    (get-metadata* (assoc opts
                          :project project
                          :overrides overrides
                          :default-repos (:repos config)))))

(defn analyze!
  "Return metadata analysis `:analysis-status` and result in `:analysis-result` file specified by `:output-filename`.
  args keys are:
  - `:project` - project artifact-id/group-id
  - `:version` - project version
  - `:jarpath` - path to jar file
  - `:pompath` - path to pom file
  - `:extra-repos` - optional additional extra maven repositories in map format: `{repo-id-here {:url \"http://repo.url.here\"}}`
  - `:output-filename` - where to write output

  This function wraps [[get-metadata]]. It does some logging and serializes result appropriately. If you want to do
  your own thing, call [[get-metadata]] directly."
  [{:keys [project version jarpath pompath output-filename] :as args}]
  {:pre [(seq project) (seq version) (seq jarpath) (seq pompath)]}
  (try
    (log/info (str "args:\n" (with-out-str (pprint/pprint args))))
    (let [output-file  (io/file output-filename)]
      (-> (get-metadata args)
          (save-result output-file))
      (log/info "results file:" (.getAbsolutePath output-file))
      (log/info "Analysis succeeded.")
      ;; TODO: consider writing these out to file well, they could be especially interesting to caller on failure
      {:analysis-status :success
       :analysis-result output-file})
    (catch Throwable t
      (let [msg (.getMessage t)]
        (log/error msg)
        (log/error "STDOUT\n" (-> t ex-data :stdout))
        (log/error "STDERR\n" (-> t ex-data :stderr))
        ;; TODO: hmmm caller is not using this info... except for analysis-status
        {:analysis-status :fail
         :fail-reason msg
         :exception (Throwable->map t)}))))
