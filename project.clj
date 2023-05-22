(defproject com.github.akond/string-template "0.0.15"

	:description "A Clojure wrapper around StringTemplate"

	:url "https://github.com/akond/string-template"

	:dependencies [[org.antlr/ST4 "4.3.4"]]

	:global-vars {*warn-on-reflection* true
				  *assert*             false}

	:license {:name         "Apache License, Version 2.0"
			  :url          "https://www.apache.org/licenses/LICENSE-2.0"
			  :distribution :repo}

	:profiles {:dev     {:dependencies [[org.clojure/clojure "1.11.1"]]}
			   :uberjar {:aot  :all
						 :dependencies [[org.clojure/clojure "1.11.1"]]
						 :main com.github.akond.string-template.main}}

	:repositories {"clojars" {:url "https://clojars.org/repo"}}

	:repl-options {:init-ns com.github.akond.string-template.core})
