#!/usr/bin/env bash

# TODO: Delete me before release


read -r -d '' OPTS << EOF
{:project "lread/cljdoc-exerciser"
 :version "1.0.19"
 :jarpath "http://repo.clojars.org/lread/cljdoc-exerciser/1.0.19/cljdoc-exerciser-1.0.19.jar"
 :pompath "http://repo.clojars.org/lread/cljdoc-exerciser/1.0.19/cljdoc-exerciser-1.0.19.pom"}
EOF

#clojure --report stderr -m cljdoc.analysis.runner-ng "{:project \"lread/cljdoc-exerciser\" :version \"1.0.19\" :jarpath \"http://repo.clojars.org/lread/cljdoc-exerciser/1.0.19/cljdoc-exerciser-1.0.19.jar\" :pompath \"http://repo.clojars.org/lread/cljdoc-exerciser/1.0.19/cljdoc-exerciser-1.0.19.pom\"}"

clojure --report stderr -m cljdoc.analysis.runner-ng "${OPTS}"
