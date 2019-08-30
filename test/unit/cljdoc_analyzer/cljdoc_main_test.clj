(ns cljdoc-analyzer.cljdoc-main-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.cljdoc-main :as cljdoc-main]))

(t/deftest cldoc-edn-test
  (t/is (= "cljdoc-edn/gid1/art1/v1/cljdoc.edn" (cljdoc-main/cljdoc-edn-filepath 'gid1/art1 "v1")))
  (t/is (= "cljdoc-edn/art2/art2/v2/cljdoc.edn" (cljdoc-main/cljdoc-edn-filepath 'art2 "v2"))))
