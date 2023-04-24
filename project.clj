(defproject com.github.akond/string-template "0.0.3"

	:description "A Clojure wrapper around StringTemplate"

	:url "https://github.com/akond/string-template"

	:dependencies [[org.clojure/clojure "1.11.1"]
				   [org.antlr/stringtemplate "4.0.2"]]

	:license {:name         "Apache License, Version 2.0"
			  :url          "https://www.apache.org/licenses/LICENSE-2.0"
			  :distribution :repo}

	:repositories {"clojars" {:url "https://clojars.org/repo"}}

	:repl-options {:init-ns com.github.akond.string-template.core})
