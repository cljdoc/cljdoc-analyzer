(ns metagetta-test.util.alter-meta-cljs
  "Hacky little utilities to support simulating import-vars type operations in cljs for test-sources"
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [clojure.java.io :as io]))

(defmacro alter-the-meta-data! [target-sym meta-changes]
  (let [target-ns (symbol (namespace target-sym))
        target-name (symbol (name target-sym))]
    (swap! env/*compiler*
           update-in [::ana/namespaces target-ns :defs target-name]
           merge meta-changes))
  nil)

(defmacro alter-the-meta-data-abs![target-sym meta-changes]
  `(alter-the-meta-data! ~target-sym ~(update meta-changes :file #(str (.getAbsolutePath (io/file %))))))

(defmacro copy-the-meta-data! [target-sym src-sym]
  (let [target-ns (symbol (namespace target-sym))
        target-name (symbol (name target-sym))
        src-ns (symbol (namespace src-sym))
        src-name (symbol (name src-sym))
        src-meta (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])]
    (swap! env/*compiler*
           update-in [::ana/namespaces target-ns :defs target-name]
           merge (dissoc src-meta :name))
    nil))

