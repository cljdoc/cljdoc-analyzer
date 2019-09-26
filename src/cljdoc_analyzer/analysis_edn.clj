(ns cljdoc-analyzer.analysis-edn
  (:refer-clojure :exclude [read])
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]))

(defn serialize
  "Return string serialized analysis result from `analysis-result`"
  [analysis-result]
  ;; the analyzed structure can contain regex #"..." (e.g. in :arglists)
  ;; and they can't be read in again using edn/read-string
  ;; so there are changed to #regex"..." and read in with a custom reader
  (->> analysis-result
       (walk/postwalk #(if (instance? java.util.regex.Pattern %)
                         (tagged-literal 'regex (str %))
                         %))
       (pr-str)))

(defn deserialize
  "Return deserialized analysis result from string `s`"
  [s]
  (edn/read-string {:readers {'regex re-pattern}} s))

(defn read
  "Return analysis result edn from `file`"
  [file]
  {:pre [(some? file)]}
  (deserialize (slurp file)))

(defn write
  "Write `analysis-result` to `file`"
  [file analysis-result]
  (spit file (serialize analysis-result)))
