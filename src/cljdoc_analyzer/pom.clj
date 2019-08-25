(ns cljdoc-analyzer.pom
  "Functions to parse POM files and extract information from them."
  (:require [clojure.string :as string])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document)))

(defn- text [^Jsoup doc sel]
  (when-let [t (some-> (.select doc sel) (first) (.ownText))]
    (when-not (string/blank? t) t)))

(defn- artifact-info [^Jsoup doc]
  ;; These `parent` fallbacks are a bit of a hack but
  ;; I didn't want to modify the data model and make this
  ;; leak outside of this namespace - Martin
  {:group-id    (or (text doc "project > groupId")
                    (text doc "project > parent > groupId"))
   :artifact-id (text doc "project > artifactId")
   :version     (or (text doc "project > version")
                    (text doc "project > parent > version"))
   :description (text doc "project > description")
   :url         (text doc "project > url")})

(defn- dependencies [^Jsoup doc]
  (for [d (.select doc "project > dependencies > dependency")]
    {:group-id    (text d "groupId")
     :artifact-id (text d "artifactId")
     :version     (text d "version")
     :scope       (text d "scope")
     :optional    (text d "optional")}))

(defn- repositories [^Jsoup doc]
  (for [r (.select doc "project > repositories > repository")]
    {:id (text r "id")
     :url (text r "url")}))

(defn parse [pom-str]
  (let [d (Jsoup/parse pom-str)]
    {:artifact-info (artifact-info d)
     :dependencies (dependencies d)
     :repositories (repositories d)}))
