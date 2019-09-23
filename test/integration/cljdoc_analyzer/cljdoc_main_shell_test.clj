(ns ^:integration cljdoc-analyzer.cljdoc-main-shell-test
  (:require [clojure.test :as t]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [cljdoc-analyzer.test-helper :as test-helper])
  (:import [java.nio.file Files]
           [java.net URI]))

(def clean-temp
  "Disable for debugging"
  true)

(def temp-dir
  (let [path (Files/createTempDirectory
              "cljdoc-test"
              (into-array java.nio.file.attribute.FileAttribute []))
        file (.toFile path)]
    (when clean-temp
      ;; This will remove temp-dir only if empty
      (.deleteOnExit file))
    file))

(defn mktemp [name]
  (let [file (io/file temp-dir name)]
    (when clean-temp
      (.deleteOnExit file))
    file))

(defn download-temp! [url name]
  (let [file (mktemp name)]
    (with-open [in  (io/input-stream (io/as-url url))
                out (io/output-stream file)]
      (io/copy in out))
    (.getAbsolutePath file)))

(defn remote->args [[project version base-url]]
  {:project project
   :version version
   :jarpath (str base-url ".jar")
   :pompath (str base-url ".pom")})

(defn local->args [[project version base-url]]
  (let [{:keys [jarpath pompath] :as args}
        (remote->args [project version base-url])
        prefix (str (string/replace project #"/" "-") "-" version)]
    (assoc args
           :jarpath (download-temp! jarpath (str prefix ".jar"))
           :pompath (download-temp! pompath (str prefix ".pom")))))

(defn- run-analysis [{:keys [project version] :as args}]
  (let [;; convention for metadata output file
        edn-out-filename (test-helper/edn-filename "/tmp/cljdoc/analysis-out/cljdoc-edn" project version)
        ;; wipe out any file from previous analysis
        _  (io/delete-file edn-out-filename true)
        args ["clojure" "--report" "stderr" "-m" "cljdoc-analyzer.cljdoc-main" (pr-str args)]]
    (println (string/join " " args))
    (println "Analyzing" project version)
    (test-helper/verify-analysis-result project version edn-out-filename (apply shell/sh args))))

(t/deftest muuntaja-unpublished-locally
  ;; known to work
  (run-analysis (local->args
                 ["metosin/muuntaja"
                  "unpublished"
                  "http://repo.clojars.org/metosin/muuntaja/0.6.3/muuntaja-0.6.3"])))

(t/deftest compojure-api-remotely
  ;; depends on ring/ring-core which requires [javax.servlet/servlet-api "2.5"]
  (run-analysis (remote->args
                 ["metosin/compojure-api"
                  "2.0.0-alpha27"
                  "http://repo.clojars.org/metosin/compojure-api/2.0.0-alpha27/compojure-api-2.0.0-alpha27"])))

(t/deftest iced-nrepl-remotely
  ;; depends on tools.namepspace 0.3.0-alpha4, cljdoc explicitly declared 0.2.11
  (run-analysis (remote->args
                 ["iced-nrepl"
                  "0.2.5"
                  "https://repo.clojars.org/iced-nrepl/iced-nrepl/0.2.5/iced-nrepl-0.2.5"])))

(t/deftest bidi-remotely
  ;; known to work
  (run-analysis (remote->args
                 ["bidi"
                  "2.1.3"
                  "http://repo.clojars.org/bidi/bidi/2.1.3/bidi-2.1.3"])))

(t/deftest orchestra-remotely
  ;; had some issues with older ClojureScript and analysis env
  (run-analysis (remote->args
                 ["orchestra"
                  "2018.11.07-1"
                  "http://repo.clojars.org/orchestra/orchestra/2018.11.07-1/orchestra-2018.11.07-1"])))

(t/deftest manifold-remotely
  ;; might have had some issues related to old versions of core.async in the past
  (run-analysis (remote->args
                 ["manifold"
                  "0.1.8"
                  "http://repo.clojars.org/manifold/manifold/0.1.8/manifold-0.1.8"])))

(t/deftest aviso-pretty-remotely
  ;; https://github.com/cljdoc/cljdoc/issues/247
  (run-analysis (remote->args
                 ["io.aviso/pretty"
                  "0.1.29"
                  "http://repo.clojars.org/io/aviso/pretty/0.1.29/pretty-0.1.29"])))

(t/deftest hx-remotely
  ;; https://github.com/lread/cljdoc-analyzer/issues/5
  ;; https://github.com/cljdoc/cljdoc/issues/289
  (run-analysis (remote->args
                 ["lilactown/hx"
                  "0.5.2"
                  "http://repo.clojars.org/lilactown/hx/0.5.2/hx-0.5.2"])))
