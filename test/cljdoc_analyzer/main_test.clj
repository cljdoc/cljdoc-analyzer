(ns cljdoc-analyzer.main-test
  "Test command line parsing and validation."
  (:require
   [cljdoc-analyzer.main :as main]
   [clojure.test :as t]
   [matcher-combinators.matchers :as m]
   [matcher-combinators.test]))

(defn main-test [args]
  (main/main* args {:dispatch-fn identity}))

;;
;; help
;;
(t/deftest cmds-usage-help-test
  (doseq [args [["help"]
                ["-h"]]]
    (t/is (match? {:exit m/absent
                   :out #"(?s)^Usage: <command>.*\nCommands:"}
                  (main-test args))
          args)))

(t/deftest run-cmd-help-test
  (doseq [args [["analyze" "-h"]
                ["analyze" "--help"]]]
    (t/is (match? {:exit m/absent
                   :out #"(?s)^Usage: analyze <options..>\n\nOptions:\n.*--project.*--output-filename"}
                  (main-test args))
          args)))

;;
;; valid args
;;
(t/deftest analyze-cmd-test
  (doseq [args [["analyze" "--project" "foo/bar" "--version" "1.2.3" "--output-filename" "out-file.edn"]
                ["analyze" "-p" "foo/bar" "-v" "1.2.3" "-o" "out-file.edn"]]]
    (t/is (match? {:exit m/absent :out m/absent
                   :cmd "analyze" :opts (m/equals {:project "foo/bar" :version "1.2.3" :output-filename "out-file.edn"})}
                  (main-test args))
          "min opts"))
  (doseq [args [["analyze" "--project" "foo/bar" "--version" "1.2.3" "--output-filename" "out-file.edn"
                 "--exclude-with" "ex1" "--exclude-with" "ex2"
                 "--extra-repo" "rid1 rurl1" "-extra-repo" "rid2 rurl2"
                 "--language" "clj" "--language" "cljs"]
                #_["analyze" "-p" "foo/bar" "-v" "1.2.3" "-o" "out-file.edn"]]]
    (t/is (match? {:exit m/absent :out m/absent
                   :cmd "analyze" :opts (m/equals {:project "foo/bar" :version "1.2.3" :output-filename "out-file.edn"
                                                   :exclude-with [:ex1 :ex2]
                                                   :extra-repo ["rid1 rurl1" "rid2 rurl2"]
                                                   :language ["clj" "cljs"]})}
                  (main-test args))
          "max opts")))

;;
;; invalid args
;;
(t/deftest cmd-missing-test
  (doseq [args [[]
                ["--foo"]]]
    (t/is (match? {:exit 1
                   :out (re-pattern (str "^.*ERRORS.*\n"
                                         " x Must specify a command.*\n"
                                         "\nUsage: <command>.*\n\n"
                                         "Commands:.*"))}
                  (main-test ["--foo"]))
          (str "args: " args))))

(t/deftest cmd-invalid-test
  (t/is (match? {:exit 1
                   :out (re-pattern (str "^.*ERRORS.*\n"
                                         " x Invalid command: bad-command\n"
                                         "\nUsage: <command>.*\n\n"
                                         "Commands:.*"))}
                  (main-test ["bad-command"]))))

(t/deftest analyze-error-test
  (t/is (match? {:exit 1
                 :out (re-pattern (str "^.*ERRORS.*?\n"
                                       " x Command does not accept args, but found: some-arg\n"
                                       " x Unrecognized option: --bad-opt\n"
                                       " x Missing required option: --project\n"
                                       " x Missing required option: --version\n"
                                       " x --extra-repo must be a quoted 'id url' pair\n"
                                       " x Missing required option: --output-filename\n"
                                       " x --language Must be one of: clj cljs\n"
                                       "\nUsage: analyze <options..>\n\n"
                                       "Options:"))}
                (main-test ["analyze" "--bad-opt" "some-val"
                            "--language" "boop"
                            "some-arg"
                            "--extra-repo" "foo"]))))
