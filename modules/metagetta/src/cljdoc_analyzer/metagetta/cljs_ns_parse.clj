(ns ^:no-doc cljdoc-analyzer.metagetta.cljs-ns-parse
  "Inlined and adapted from clojure.tools.namespace.parse.
  Not necessarily cljs specific, but modified solely to support cljs analysis; it now
  returns npm deps (JavaScript requires) instead of skipping over them.")

;; code unchanged from original
(defn- prefix-spec?
  "Returns true if form represents a libspec prefix list like
  (prefix name1 name1) or [com.example.prefix [name1 :as name1]]"
  [form]
  (and (sequential? form)  ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form))))  ; not a bare vector like [foo]

;; code unchanged from original
(defn- option-spec?
  "Returns true if form represents a libspec vector containing optional
  keyword arguments like [namespace :as alias] or
  [namespace :refer (x y)] or just [namespace]"
  [form]
  (and (sequential? form)  ; should be a vector, but often is not
       (or (symbol? (first form))
           (string? (first form)))
       (or (keyword? (second form))  ; vector like [foo :as f]
           (= 1 (count form)))))  ; bare vector like [foo]

;; changed from original
(defn- deps-from-libspec [prefix form]
  (cond
    (prefix-spec? form)
    (mapcat (fn [f] (deps-from-libspec
                     (symbol (str (when prefix (str prefix "."))
                                  (first form)))
                     f))
            (rest form))

    (option-spec? form)
    (when-not (= :as-alias (second form))
      (deps-from-libspec prefix (first form)))

    (symbol? form)
    (list (symbol (str (when prefix (str prefix ".")) form)))

    (keyword? form)  ; Some people write (:require ... :reload-all)
    nil

    ;; CHANGED from clojure.tools.namespace/parse, it returns nil, we return the string lib
    (string? form) ; NPM dep, aka JavaScript require.
    (list form)

    :else
    (throw (ex-info "Unparsable namespace form"
                    {:reason ::unparsable-ns-form
                     :form form}))))

;; code unchanged from original
(def ^:private ns-clause-head-names
  "Set of symbol/keyword names which can appear as the head of a
  clause in the ns form."
  #{"use" "require" "require-macros"})

;; code unchanged from original
(def ^:private ns-clause-heads
  "Set of all symbols and keywords which can appear at the head of a
  dependency clause in the ns form."
  (set (mapcat (fn [name] (list (keyword name)
                                (symbol name)))
               ns-clause-head-names)))

;; code unchanged from original
(defn- deps-from-ns-form [form]
  (when (and (sequential? form)  ; should be list but sometimes is not
             (contains? ns-clause-heads (first form)))
    (mapcat #(deps-from-libspec nil %) (rest form))))

;; code unchanged from original
(defn deps-from-ns-decl
  "Given an (ns...) declaration form (unevaluated), returns a set of
  symbols naming the dependencies of that namespace.  Handles :use and
  :require clauses but not :load."
  [decl]
  (set (mapcat deps-from-ns-form decl)))
