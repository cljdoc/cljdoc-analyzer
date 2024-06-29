(ns ^:no-doc cljdoc-analyzer.config
  (:refer-clojure :exclude [load])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn load[]
  (edn/read-string (slurp (io/resource "config.edn"))))
