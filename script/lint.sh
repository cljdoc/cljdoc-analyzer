#!/usr/bin/env bash

# cljdoc-analyzer internal metagetta sub-project is linted along with cljdoc-analyzer

set -eou pipefail

function lint() {
    local lint_args
    if [ ! -d .clj-kondo/.cache ]; then
        echo "--[linting and building cache]--"
        # classpath with tests paths
        local classpath;classpath="$(clojure -R:test -C:test -Spath)"
        lint_args="$classpath metagetta"
    else
        echo "--[linting]--"
        lint_args="src test metagetta"
    fi
    set +e
    clojure -A:clj-kondo --lint ${lint_args}
    local exit_code=$?
    set -e
    if [ ${exit_code} -ne 0 ] && [ ${exit_code} -ne 2 ] && [ ${exit_code} -ne 3 ]; then
        echo "** clj-kondo exited with unexpected exit code: ${exit_code}"
    fi
    exit ${exit_code}
}

lint
