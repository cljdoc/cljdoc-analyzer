(ns cljdoc-analyzer.util-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.util :as util]))

(t/deftest group-id-test
  (t/is (= "a-group-id" (util/group-id "a-group-id/an-artifact-id")))
  (t/is (= "b-group-id" (util/group-id 'b-group-id/an-artifact-id)))
  (t/is (= "c-group-id" (util/group-id "c-group-id")))
  (t/is (= "d-group-id" (util/group-id 'd-group-id))))

(t/deftest artifact-id-test
  (t/is (= "a-an-artifact-id" (util/artifact-id "a-group-id/a-an-artifact-id")))
  (t/is (= "b-an-artifact-id" (util/artifact-id 'a-group-id/b-an-artifact-id)))
  (t/is (= "a-group-id" (util/artifact-id "a-group-id")))
  (t/is (= "b-group-id" (util/artifact-id 'b-group-id))))



(t/deftest serialize-cldoc-edn-test
  (t/is (= "{:regex-test #regex \".*booya.*\"}" (util/serialize-cljdoc-edn {:regex-test #".*booya.*"})))
  (t/is (= "{:i {:am {:deeper {:yes #regex \"(ok|good)\", :other-stuff \"bingo\"}}}}"
           (util/serialize-cljdoc-edn {:i {:am {:deeper {:yes #"(ok|good)" :other-stuff "bingo"}}}}))))

(t/deftest deserialize-cljdoc-edn-test
  ;; we convert to string because in clojure: (= #"booya" #"booya") evals to false but (= (str #"booya") (str #"booya")) evals to true
  (t/is (= (str {:regex-test #".*booya.*"})
           (str (util/deserialize-cljdoc-edn "{:regex-test #regex \".*booya.*\"}"))))
  (t/is (= (str {:i {:am {:deeper {:yes #"(ok|good)" :other-stuff "bingo"}}}})
           (str (util/deserialize-cljdoc-edn "{:i {:am {:deeper {:yes #regex \"(ok|good)\", :other-stuff \"bingo\"}}}}")))))

(t/deftest clojars-id-test
  (t/is (= "grp1/art1" (util/clojars-id {:group-id "grp1" :artifact-id "art1"})))
  (t/is (= "art1" (util/clojars-id {:group-id "art1" :artifact-id "art1"}))))
