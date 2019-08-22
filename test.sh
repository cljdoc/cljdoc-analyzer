#!/usr/bin/env bash
# TODO: Delete me before release

set -euo pipefail

function analyze() {
    local group=$1
    local artifact=$2
    local version=$3
    local repo_prefix="http://repo.clojars.org/${group}/${artifact}/${version}/${artifact}-${version}"
    OPTS=$(cat <<EOM
{:project "${group}/${artifact}"
 :version "${version}"
 :jarpath "${repo_prefix}.jar"
 :pompath "${repo_prefix}.pom"}
EOM
)
    echo ${OPTS}
    clojure --report stderr -m cljdoc.analysis.runner-ng "${OPTS}"
}

#analyze "lread" "cljdoc-exerciser" "1.0.19"
analyze "cljfmt" "cljfmt" "0.6.4"
