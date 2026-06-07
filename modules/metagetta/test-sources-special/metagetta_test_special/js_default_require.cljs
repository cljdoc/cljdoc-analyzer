(ns metagetta-test-special.js-default-require
  "A string require that targets a JS default export via the `module$default`
  form. ClojureScript resolves it against the base module
  (\"some-js-lib/widget\"), which the analyzer must find stubbed in its
  :js-dependency-index. https://github.com/cljdoc/cljdoc-analyzer/issues/18"
  (:require ["some-js-lib/widget$default" :as Widget]))

(defn make-widget
  "Construct a Widget, referencing the default-exported JS class."
  []
  (Widget.))
