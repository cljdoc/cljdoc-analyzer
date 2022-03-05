(ns release
  (:require [clojure.string :as string]
            [helper.git :as git]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- readme-update [tag-version]
  (status/line :detail "Updating readme")
  (let [readme (slurp "README.adoc")
        new-readme (string/replace-first
                     readme
                     #":library-version: (.*)"
                     (str ":library-version: " tag-version))]
    (spit "README.adoc" new-readme)
    (status/line :detail "Readme update for %s" tag-version)))

(defn- change-log-update [tag-version]
  (status/line :detail "Updating change log")
  (let [log (slurp "CHANGELOG.adoc")
        new-log (string/replace-first
                 log
                 #"(?ms)^== Unreleased *$"
                 (str "== Unreleased\n\n*\n\n== " tag-version))]
    (spit "CHANGELOG.adoc" new-log)
    (status/line :detail "Change log updated for %s" tag-version)))

(defn change-log-check []
  (status/line :detail "Checking change log")
  (let [log (slurp "CHANGELOG.adoc")
        new-changes (last (re-find #"(?ms)^== Unreleased *$(.*?)(?:^==|\z)" log))]
    (if (not (and new-changes (re-find #"(?ims)[a-z]" new-changes)))
      (status/die 1 "Must contain Unreleased section with some text.")
      (println "PASS: Unreleased section found with some text."))))

(defn release-checks []
  (status/line :head "Performing release checks")
  (when (not= "master" (git/current-branch))
    (status/die 1 "Can only cut a release from the main branch"))
  (when (git/uncommitted-code?)
      (status/die 1 "Please commit all code before cutting a release."))
  (when (git/unpushed-commits?)
      (status/die 1 "Please push all commits before cutting a release."))
  (change-log-check)
  (status/line :detail "All checks passed"))

(defn- version-tag [version]
  (str "v" version))

(defn- update-files [version]
  (let [tag-version (version-tag version)]
    (status/line :head "Updating files for %s" tag-version)
    (spit "resources/cljdoc-analyzer-version.edn" (pr-str {:version version}))
    (readme-update tag-version)
    (change-log-update tag-version)))

(defn- update-repo [version]
  (let [tag-version (version-tag version)]
    (status/line :head "Updating git repo for %s" version)
    (status/line :detail "Adding changes")
    (shell/command "git add"
                   "resources/cljdoc-analyzer-version.edn"
                   "README.adoc"
                   "CHANGELOG.adoc")
    (status/line :detail "Comitting")
    (shell/command "git commit -m" (str "Release " tag-version))
    (status/line :detail "Version tagging")
    (shell/command "git tag -a" tag-version "-m" (str "Release " tag-version))
    (shell/command "git tag -f RELEASE")
    ))

(defn- push-repo [version]
  (let [tag-version (version-tag version)]
    (status/line :detail "Pushing changes")
    (shell/command "git push")
    (shell/command "git push origin" tag-version)
    (shell/command "git push -f origin RELEASE")))

(defn release
  "Coordinated by bb.edn, assumes tests and release-checks have been performed"
  []
  (let [commit-count (git/commit-count)
        version (str "1.0." (inc commit-count))]
    (update-files version)
    (update-repo version)
    (push-repo version)))

(comment
  (update-files "1.2.3")
  (change-log-update "1.2.3")

  )
