(ns cljdoc-analyzer.analysis-edn-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.analysis-edn :as analysis-edn]))

(t/deftest serialize-cldoc-edn-test
  (t/is (= "{:regex-test #regex \".*booya.*\"}" (analysis-edn/serialize {:regex-test #".*booya.*"})))
  (t/is (= "{:i {:am {:deeper {:yes #regex \"(ok|good)\", :other-stuff \"bingo\"}}}}"
           (analysis-edn/serialize {:i {:am {:deeper {:yes #"(ok|good)" :other-stuff "bingo"}}}}))))

(t/deftest deserialize-cljdoc-edn-test
  ;; we convert to string because in clojure: (= #"booya" #"booya") evals to false but (= (str #"booya") (str #"booya")) evals to true
  (t/is (= (str {:regex-test #".*booya.*"})
           (str (analysis-edn/deserialize "{:regex-test #regex \".*booya.*\"}"))))
  (t/is (= (str {:i {:am {:deeper {:yes #"(ok|good)" :other-stuff "bingo"}}}})
           (str (analysis-edn/deserialize "{:i {:am {:deeper {:yes #regex \"(ok|good)\", :other-stuff \"bingo\"}}}}")))))
