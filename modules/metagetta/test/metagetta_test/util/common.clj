(ns metagetta-test.util.common
  (:require [clojure.string :as string]))

(defn tweak [meta-changes]
  (if (:file meta-changes)
    (update meta-changes
            :file
            #(string/replace %
                             "{metagetta.test.sources.root}"
                             (System/getProperty "metagetta.test.sources.root" "test-sources")))
    meta-changes))
