(ns helper.git
  (:require [clojure.string :as string]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn uncommitted-code? []
  (-> (shell/command {:out :string}
                     "git status --porcelain")
      :out
      string/trim
      seq))

(defn unpushed-commits? []
  (let [{:keys [:exit :out]} (shell/command {:continue true :out :string}
                                            "git cherry -v")]
    (if (zero? exit)
      (-> out string/trim seq)
      (status/die 1 "Failed to check for unpushed commits to branch, is your branch pushed?"))))


(defn current-branch []
  (-> (shell/command {:out :string}
                     "git rev-parse --abbrev-ref HEAD")
      :out
      string/trim))

(defn commit-count []
  (-> (shell/command {:out :string}
                     "git rev-list HEAD --count")
      :out
      string/trim
      parse-long))
