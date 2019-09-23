(ns cljdoc-analyzer.analysis-edn
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]))

(defn serialize-cljdoc-edn
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

(defn deserialize-cljdoc-edn
  "Return deserialized analysis result from string `s`"
  [s]
  (edn/read-string {:readers {'regex re-pattern}} s))

(defn read-cljdoc-edn
  "Return analysis result edn from `file`"
  [file]
  {:pre [(some? file)]}
  (deserialize-cljdoc-edn (slurp file)))

(defn write-cljdoc-edn
  "Write `analysis-result` to `file`"
  [file analysis-result]
  (spit file (serialize-cljdoc-edn analysis-result)))
