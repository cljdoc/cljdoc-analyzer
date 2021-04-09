(ns metagetta-test.util.alter-meta-clj
  "Hacky little utilities to support simulating import-vars type operations in clj for test-sources"
  (:require [clojure.java.io :as io]))

(defmacro alter-the-meta-data![target-sym meta-changes]
(let [target-ns (symbol (namespace target-sym))
      target-name (symbol (name target-sym))]
  (alter-meta! (ns-resolve target-ns target-name) merge meta-changes)
  nil))

(defmacro alter-the-meta-data-abs![target-sym meta-changes]
  `(alter-the-meta-data! ~target-sym ~(update meta-changes :file #(str (.getAbsolutePath (io/file %))))))


(defn resolve-fn-location[var-meta]
  (if-let [p (:protocol var-meta)]
    (-> (meta p)
        (select-keys [:file :line])
        (merge var-meta))
    var-meta))

(defmacro copy-the-meta-data! [target-sym source-sym]
  (let [target-ns (symbol (namespace target-sym))
        target-name (symbol (name target-sym))]
    (alter-meta! (ns-resolve target-ns target-name) merge (dissoc (resolve-fn-location (meta (resolve source-sym))) :name))
    nil))
