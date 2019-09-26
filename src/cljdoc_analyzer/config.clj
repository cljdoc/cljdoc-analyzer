(ns ^:no-doc cljdoc-analyzer.config
  (:refer-clojure :exclude [load])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load[]
  (edn/read-string (slurp (io/resource "config.edn"))))
