;;
;; Note: this file is used in tests that rely on line numbers
;;
(ns metagetta-test.test-ns1.meta)

(defn arglists1 {:arglists '([al1])} [orig args])
(defn arglists2 {:arglists '([al2 bl2] [al2 bl2 cl2])} ([a b]) ([a b c]))
(defn arglists3 ([orig args]) {:arglists '([al3])})
(defn arglists4 {:arglists '([al4x2])} ([orig args]) {:arglists '([al4])})

(defn doc1 "doc1" [])
(defn ^{:doc "doc2"} doc2 [])
(defn doc3 {:doc "doc3"} [])
(defn doc4 ([]) {:doc "doc4"})
(defn ^{:doc "doc5x1"} doc5 "doc5x2" {:doc "doc5x3"} ([]) {:doc "doc5"})

(defn ^{:doc "twx1"} theworks "twx2"
  {:arglists '([twx3]) :doc "twx3"}
  ([orig args]) {:arglists '([tw])
                 :doc "tw"})
