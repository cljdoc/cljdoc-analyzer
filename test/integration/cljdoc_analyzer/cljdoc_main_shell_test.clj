(ns ^:integration cljdoc-analyzer.cljdoc-main-shell-test
  "These tests mimic the way cljdoc calls cljdoc-analyzer."
  (:require [clojure.test :as t]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [cljdoc-analyzer.test-helper :as test-helper]
            [babashka.fs :as fs]))

(def clean-temp
  "Disable for debugging"
  true)

(def temp-dir
  (let [dir (fs/create-temp-dir {:prefix "cljdoc-test"})]
    (when clean-temp
      ;; This will remove temp-dir only if empty
      (fs/delete-on-exit dir))
    dir))

(defn temp-file [name]
  (let [file (fs/file temp-dir name)]
    (when clean-temp
      (fs/delete-on-exit file))
    file))

(defn download-temp! [url name]
  (let [file (temp-file name)]
    (with-open [in  (io/input-stream (io/as-url url))
                out (io/output-stream file)]
      (io/copy in out))
    (-> file fs/absolutize str)))

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
        edn-out-filename (test-helper/edn-filename "/tmp/cljdoc/analysis-out/cljdoc-analysis-edn" project version)
        ;; wipe out any file from previous analysis
        _  (fs/delete-if-exists edn-out-filename)
        args ["clojure" "-M" "--report" "stderr" "-m" "cljdoc-analyzer.cljdoc-main" (pr-str args)]]
    (println (string/join " " args))
    (println "Analyzing" project version)
    (test-helper/verify-analysis-result project version edn-out-filename (apply shell/sh args))))

(t/deftest re-frame-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/18
  ;; https://github.com/cljdoc/cljdoc/issues/289
  (run-analysis (remote->args
                 ["re-frame"
                  "0.12.0"
                  "https://repo.clojars.org/re-frame/re-frame/0.12.0/re-frame-0.12.0"])))

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

(t/deftest specter-remotely
  ;; https://github.com/cljdoc/cljdoc/issues/261
  (run-analysis (remote->args
                 ["com.rpl/specter"
                  "1.1.3"
                  "https://repo.clojars.org/com/rpl/specter/1.1.3/specter-1.1.3"])))

(t/deftest roughcljs-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/18
  ;; https://github.com/cljdoc/cljdoc/issues/289
  (run-analysis (remote->args
                 ["frozar/roughcljs"
                  "0.2.4"
                  "https://repo.clojars.org/frozar/roughcljs/0.2.4/roughcljs-0.2.4"])))

(t/deftest cli-matic-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/26
  (run-analysis (remote->args
                 ["cli-matic"
                  "0.4.3"
                  "http://repo.clojars.org/cli-matic/cli-matic/0.4.3/cli-matic-0.4.3"])))

(t/deftest gorilla-ui-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/pull/25
  ;; https://github.com/cljdoc/cljdoc/issues/358
  (run-analysis (remote->args
                 ["org.pinkgorilla/gorilla-ui"
                  "0.1.66"
                  "http://repo.clojars.org/org/pinkgorilla/gorilla-ui/0.1.66/gorilla-ui-0.1.66"])))

 (t/deftest cheshire-remotely
   ;; https://github.com/cljdoc/cljdoc/issues/360
   (run-analysis (remote->args
                  ["cheshire"
                   "5.9.0"
                   "https://repo.clojars.org/cheshire/cheshire/5.9.0/cheshire-5.9.0"])))

(t/deftest clj-branca-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/29
  ;; https://github.com/cljdoc/cljdoc/issues/404 - but oz still has some
  ;; unrelated issues, clj-branca works today.
  (run-analysis (remote->args
                 ["miikka/clj-branca"
                  "0.1.0"
                  "https://repo.clojars.org/miikka/clj-branca/0.1.0/clj-branca-0.1.0"])))

(t/deftest clojure-tools-deps-remotely
  ;; https://github.com/cljdoc/cljdoc/issues/742
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/66
  (run-analysis (remote->args
                  ["org.clojure/tools.deps"
                   "0.16.1281"
                   "https://repo1.maven.org/maven2/org/clojure/tools.deps/0.16.1281/tools.deps-0.16.1281"])))

(t/deftest clojure-1-11-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/53
  (run-analysis (remote->args
                  ["org.clojure/clojure"
                   "1.11.1"
                   "https://repo1.maven.org/maven2/org/clojure/clojure/1.11.1/clojure-1.11.1"])))

(t/deftest clojure-1-7-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/53
  (run-analysis (remote->args
                  ["org.clojure/clojure"
                   "1.7.0"
                   "https://repo1.maven.org/maven2/org/clojure/clojure/1.7.0/clojure-1.7.0"])))

(t/deftest instparse-1-4-12-remotely
  ;; https://github.com/cljdoc/cljdoc-analyzer/issues/101
  (run-analysis (remote->args
                  ["instaparse/instaparse"
                   "1.4.12"
                   "https://repo.clojars.org/instaparse/instaparse/1.4.12/instaparse-1.4.12"])))
