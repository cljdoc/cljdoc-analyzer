(ns cljdoc-analyzer.spec
  (:refer-clojure :exclude [assert])
  (:require [clojure.spec.alpha :as s]))
;; TODO: while testing leave this on
(s/check-asserts true)

(s/def ::platform #{"clj" "cljs"})

;; codox -------------------------------------------------------------
;; A spec for Codox namespace analysis data

(s/def :cljdoc.codox.public/name symbol? #_(s/or :a string? :b symbol?))
(s/def :cljdoc.codox.public/file string?)
(s/def :cljdoc.codox.public/line int?)
(s/def :cljdoc.codox.public/arglists coll?)
(s/def :cljdoc.codox.public/doc (s/nilable string?))
(s/def :cljdoc.codox.public/type #{:var :fn :macro :protocol :multimethod})
(s/def :cljdoc.codox.public/members (s/coll-of :cljdoc.codox/public))

(s/def :cljdoc.codox/public
  (s/keys :req-un [:cljdoc.codox.public/name
                   :cljdoc.codox.public/type]
          :opt-un [:cljdoc.codox.public/deprecated
                   :cljdoc.codox.public/doc
                   :cljdoc.codox.public/arglists
                   :cljdoc.codox.public/file
                   :cljdoc.codox.public/line
                   :cljdoc.codox.public/members]))

(s/def :cljdoc.codox.namespace/name symbol?)
(s/def :cljdoc.codox.namespace/publics (s/coll-of :cljdoc.codox/public))
(s/def :cljdoc.codox.namespace/doc (s/nilable string?))

(s/def :cljdoc.codox/namespace
  (s/keys :req-un [:cljdoc.codox.namespace/name
                   :cljdoc.codox.namespace/publics]
          :opt-un [:cljdoc.codox.namespace/doc]))


;; cljdoc.edn ---------------------------------------------------------

(s/def :cljdoc.cljdoc-edn/codox
  (s/map-of ::platform (s/coll-of :cljdoc.codox/namespace)))

(s/def :cljdoc.cljdoc-edn/pom-str string?)

(s/def :cljdoc/cljdoc-edn
  (s/keys :req-un [:cljdoc.cljdoc-edn/codox
                   :cljdoc.cljdoc-edn/pom-str]))

;; utilities ----------------------------------------------------------

(defn assert [spec v]
  (if (s/get-spec spec)
    (s/assert spec v)
    (throw (Exception. (format "No spec found for %s" spec)))))
