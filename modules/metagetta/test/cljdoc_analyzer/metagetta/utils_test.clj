(ns cljdoc-analyzer.metagetta.utils-test
  (:require
   [cljdoc-analyzer.metagetta.utils :as utils]
   [clojure.test :as t])
  (:import
   [java.time Duration]) )

(t/deftest humanize-duration-test
  (t/is (= "1s" (utils/humanize-duration (Duration/ofMillis 1000))))
  (t/is (= "1.234s" (utils/humanize-duration (Duration/ofMillis 1234))))
  (t/is (= "7200h" (utils/humanize-duration (Duration/ofDays 300))))
  (t/is (= "308641975h 37m 2.334s"
           (utils/humanize-duration
             (Duration/ofMillis (+ (* 308641975 60 60 1000)
                                   (* 37 60 1000)
                                   2334))))))
