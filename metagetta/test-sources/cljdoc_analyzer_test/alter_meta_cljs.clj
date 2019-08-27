;; TODO: move this support outside of analyzed sources
(ns ^:no-doc cljdoc-analyzer-test.alter-meta-cljs
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [clojure.java.io :as io]))

(defmacro alter-the-meta-data![ns name meta-changes]
  (swap! env/*compiler*
         update-in [::ana/namespaces ns :defs name]
         merge meta-changes)
  nil)

(defmacro alter-the-meta-data-abs![ns name meta-changes]
  `(alter-the-meta-data! ~ns ~name ~(update meta-changes :file #(str (.getAbsolutePath (io/file %))))))

(defmacro copy-the-meta-data! [target-ns target-name src-sym]
  (let [src-ns (symbol (namespace src-sym))
        src-name (symbol (name src-sym))
        src-meta (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])]
    (swap! env/*compiler*
           update-in [::ana/namespaces target-ns :defs target-name]
           merge (dissoc src-meta :name))
    nil))
