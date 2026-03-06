(ns ^:no-doc cljdoc-analyzer.main
  (:require
   [babashka.cli :as cli]
   [cljdoc-analyzer.config :as config]
   [cljdoc-analyzer.deps :as deps]
   [cljdoc-analyzer.runner :as runner]
   [clojure.string :as string]))

(defn- extra-repo-arg-to-option [extra-repo]
  (reduce (fn [acc n]
            (let [[id url] (string/split n #" ")]
              (assoc acc id {:url url})))
          {}
          extra-repo))

(defn analyze [{:keys [opts]}]
  (let [{:keys [project version extra-repo language] :as opts} opts
        config (config/load)
        extra-repos (extra-repo-arg-to-option extra-repo)
        languages (when (seq language) (into #{} language))
        {:keys [jar pom]} (deps/resolve-artifact (symbol project) version (:repos config) extra-repos)]
    (runner/analyze! (-> (merge
                          {:exclude-with [:no-doc :skip-wiki :mranderson/inlined]}
                          (select-keys opts [:project :version :exclude-with :output-filename]))
                         (assoc :jarpath jar :pompath pom :extra-repos extra-repos :languages languages)))))

(defn kw-opt->cli-opt
  [kw-opt]
  (let [opt (name kw-opt)]
    (if (= 1 (count opt))
      (str "-" opt)
      (str "--" opt))))

(def valid-languages ["clj" "cljs"])

(def analyze-spec
  {:project
   {:desc "Project to analyze"
    :ref "<group-id/artifact-id>"
    :alias :p
    :coerce :string
    :require true}
   :version
   {:desc "Project version to analyze"
    :ref "<version>"
    :alias :v
    :coerce :string
    :require true}
   :exclude-with
   {:desc "Exclude namespaces and publics with metadata key present, repeat for multiple"
    :ref "<metadata key>"
    :alias :e
    :coerce [:keyword]}
   :extra-repo
   {:desc "Include extra maven repo using quoted syntax 'repo-id repo-url', repeat for multiple"
    :ref "<'repo-id repo-url'>"
    :alias :r
    :validate {:pred #(every? (fn [v] (= 2 (count (string/split v #" ")))) %)
               :ex-msg (fn [{:keys [option]}] (format "%s must be a quoted 'id url' pair"
                                                      (kw-opt->cli-opt option)))} 
    :coerce [:string]}
   :output-filename
   {:desc "Where to write edn output"
    :ref "<filename>"
    :alias :o
    :coerce :string
    :require true}
   :language
   {:desc "Language to analyze, omit for auto detection, repeat for multiple"
    :ref (format "<%s>" (string/join "|" valid-languages))
    :alias :l
    :validate {:pred #(every? (set valid-languages) %)
               :ex-msg (fn [{:keys [option]}] (format "%s Must be one of: %s"
                                                      (kw-opt->cli-opt option)
                                                      (string/join " " valid-languages)))}
    :coerce [:string]}
   :help
   {:desc "Help"
    :alias :h
    :coerce :boolean}})

(def analyze-opt-order [:project :version :exclude-with :extra-repo :output-filename :language :help])

(def table
  [{:cmd "analyze"
    :fn analyze
    :spec analyze-spec
    :usage-opt-order analyze-opt-order}])

(def cmds-help "Usage: <command> [options...]

Commands:

analyze          Analyzes jar and returns public API as namespaces and vars 

Use <command> --help for help on command")

(defn- opts->table
  "Customized bb cli opts->table for cljdoc"
  [{:keys [spec order]}]
  (mapv (fn [[long-opt {:keys [alias default default-desc desc extra-desc ref require]}]]
          (let [alias (when alias
                        (str (kw-opt->cli-opt alias)))
                option (kw-opt->cli-opt long-opt)
                default-shown (or default-desc
                                  default)
                attribute (or (when require "*required*")
                              default-shown)
                desc-shown (cond-> [(if attribute
                                      (str desc " [" attribute "]")
                                      desc)]
                             extra-desc (into extra-desc))]
            [(str (when alias (str alias ", "))
                  option
                  (when ref (str " " ref)))
             (string/join "\n " desc-shown)]))
        (let [order (or order (keys spec))]
          (map (fn [k] [k (spec k)]) order))))

(defn format-opts
  "Customized bb cli format-opts for cljdoc"
  [{:as cfg}]
  (cli/format-table {:rows (opts->table cfg) :indent 1}))

(defn error-text [text]
  (str "\u001B[31m" text "\u001B[0m"))

(defn opts-error-msg [{:keys [cause msg option]}]
  ;; Override default: options in cmdline syntax, not as keywords
  (cond
    (= :require cause)
    (str "Missing required option: " (kw-opt->cli-opt option))
    (= :restrict cause)
    (str "Unrecognized option: " (kw-opt->cli-opt option))
    :else msg))

(defn cmd-def-from-cmd [cmd-table cmd-find]
  (some (fn [{:keys [cmd cmd-alias] :as cmd-def}]
          (when (or (= cmd cmd-find)
                    (and cmd-alias (= cmd-alias cmd-find)))
            cmd-def))
        cmd-table))

(defn parse-cmd-opts-args
  [cli-args opts]
  (let [{:keys [args opts]} (cli/parse-args cli-args opts)]
    {:cmd (first args)
     :args (rest args)
     :opts opts}))

(defn parse-cmd [cmd-table cli-args]
  (let [{:keys [cmd args]} (parse-cmd-opts-args cli-args {})
        cmd-def (cmd-def-from-cmd cmd-table cmd)]
    (cond
      (nil? cmd)
      {:errors [{:msg "Must specify a command"}]}

      (nil? cmd-def)
      {:errors [{:msg (str "Invalid command: " cmd)}]}

      (seq args)
      {:cmd (:cmd cmd-def) :errors [{:msg (str "Command does not accept args, but found: " (first args))}]}

      :else
      {:cmd (:cmd cmd-def)})))

(defn cmds-help-requested [cli-args]
  (let [{:keys [cmd opts]} (parse-cmd-opts-args cli-args {:aliases {:h :help}})]
    (when (or (and (not (seq opts)) (= "help" cmd))
              (and (not cmd) (= {:help true} opts)))
      cmds-help)))

(defn cmd-usage-help [{:keys [spec usage-opt-order cmd]}]
  (if (seq usage-opt-order)
    (str "Usage: " cmd " <options..>\n\nOptions:\n\n"
         (format-opts {:spec spec :order usage-opt-order}))
    (str "Usage: " cmd "\n\nOptions: none for this command")))

(defn cmd-help-requested [cmd-table cli-args]
  (let [{:keys [cmd opts]} (parse-cmd-opts-args cli-args {:aliases {:h :help}})]
    (when (and (cmd-def-from-cmd cmd-table cmd) (:help opts))
      (cmd-usage-help (cmd-def-from-cmd cmd-table cmd)))))

(defn errors-as-text [errors usage-help]
  (str (error-text "ERRORS:") "\n"
       (reduce (fn [acc e]
                 (str acc " x " e "\n"))
               ""
               errors)
       "\n"
       usage-help
       "\n"))

(defn sort-errors
  "Sort errors by msg then by usage-opt-order"
  [cmd-def all-errors]
  (let [opt-order (zipmap (:usage-opt-order cmd-def) (range))]
    (sort (fn [x y]
            (let [x-opt-order (get opt-order (:option x))
                  y-opt-order (get opt-order (:option y))
                  c (compare x-opt-order y-opt-order)]
              (if (not= 0 c)
                c
                (compare (:msg x) (:msg y)))))
          all-errors)))

(defn main*
  "Separated out for testing. `:dipatch-fn` supports testing and overrides cmd dispatch `:fn`, set to, for example `identity`."
  [cli-args {:keys [dispatch-fn]}]
  ;; bb cli has a dispatch, but it can't currenlty do what we want, so we do our own thing
  (if-let [help (cmds-help-requested cli-args)]
    {:out help}
    (let [opt-errors (atom [])
          cmd-table (mapv (fn [{:keys [spec] :as d}]
                            (assoc d
                                   :spec (assoc spec :help {:alias :h})
                                   :error-fn (fn opts-error-fn [{:keys [msg option] :as data}]
                                               (if-let [refined-msg (opts-error-msg data)]
                                                 (swap! opt-errors conj {:option option :msg refined-msg})
                                                 (throw (ex-info msg data))))
                                   :restrict true))
                          table)]
      (if-let [help (cmd-help-requested cmd-table cli-args)]
        {:out help}
        (let [{:keys [cmd errors]} (parse-cmd cmd-table cli-args)
              cmd-def (cmd-def-from-cmd cmd-table cmd)
              cmd-opts-args (when cmd-def (parse-cmd-opts-args cli-args cmd-def))
              all-errors (cond-> []
                           errors (into errors)
                           @opt-errors (into @opt-errors))]
          (if (seq all-errors)
            {:out (errors-as-text (->> all-errors
                                       (sort-errors cmd-def)
                                       (mapv :msg))
                                  (if cmd-def
                                    (cmd-usage-help cmd-def)
                                    cmds-help))
             :exit 1}
            ((or dispatch-fn (:fn cmd-def)) cmd-opts-args)))))))

(defn -main
  [& cli-args]
  (let [{:keys [exit out]} (main* cli-args {})]
    (when out
      (println out))
    (if exit
      (System/exit exit)
      (shutdown-agents))))
