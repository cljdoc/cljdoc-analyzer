(ns cljdoc-analyzer.metagetta.utils-test
  (:require
   [cljdoc-analyzer.metagetta.utils :as utils]
   [clojure.string :as str]
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

(defn- ci [lines]
  (str/split-lines
   (utils/correct-indent (str/join "\n" lines))))

(t/deftest correct-indent-test
  (t/is (= nil (utils/correct-indent "")))
  (t/is (= ["hello"]
           (ci ["hello"])))
  (t/is (= ["line1"
            "line2"]
           (ci ["line1"
                "line2"])))
  (t/is (= ["line1"
            "line2"]
           (ci ["line1"
                "  line2"])))
  (t/is (= ["Returns `:boop`."
            ""
            "Some other note here."]
           (ci ["Returns `:boop`."
                ""
                "     Some other note here."])))
  (t/is (= ["First line."
            ""
            "Second line."
            "Third line."]
           (ci ["First line."
                ""
                "  Second line."
                "  Third line."])))
  (t/is (= ["First line."
            ""
            " Second line."
            "Third line."]
           (ci ["First line."
                ""
                "   Second line."
                "  Third line."]))))
