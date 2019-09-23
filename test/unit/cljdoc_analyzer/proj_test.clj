(ns cljdoc-analyzer.proj-test
  (:require [clojure.test :as t]
            [cljdoc-analyzer.proj :as proj]))

(t/deftest group-id-test
  (t/is (= "a-group-id" (proj/group-id "a-group-id/an-artifact-id")))
  (t/is (= "b-group-id" (proj/group-id 'b-group-id/an-artifact-id)))
  (t/is (= "c-group-id" (proj/group-id "c-group-id")))
  (t/is (= "d-group-id" (proj/group-id 'd-group-id))))

(t/deftest artifact-id-test
  (t/is (= "a-an-artifact-id" (proj/artifact-id "a-group-id/a-an-artifact-id")))
  (t/is (= "b-an-artifact-id" (proj/artifact-id 'a-group-id/b-an-artifact-id)))
  (t/is (= "a-group-id" (proj/artifact-id "a-group-id")))
  (t/is (= "b-group-id" (proj/artifact-id 'b-group-id))))

(t/deftest clojars-id-test
  (t/is (= "grp1/art1" (proj/clojars-id {:group-id "grp1" :artifact-id "art1"})))
  (t/is (= "art1" (proj/clojars-id {:group-id "art1" :artifact-id "art1"}))))
