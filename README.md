[![Clojars Project](https://img.shields.io/clojars/v/com.github.akond/string-template.svg)](https://clojars.org/com.github.akond/string-template)

A Clojure wrapper around StringTemplate.

This is a Proof of concept. Do not use in production.

# Rationale
* A paper "Enforcing Strict Model-View Separation in Template Engines" by Terence Parr.

# Resources
* [Template Syntax cheet sheet](https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md)

# Examples
## Hello, world
```clojure
(require '[com.github.akond.string-template.core :as st])
(let [t (st/template "Hello, <name>!")]
	(-> t (merge {:name "world"}) str))
=> "Hello, world!"
```

## Using template groups
```clojure
(let [g (st/group (io/file "example.stg"))]
	(-> g :greet (merge {:hellos ["Hi" "Guten morgen" "Buenos dias"]}) print))
=>
Hi, world!
Guten morgen, world!
Buenos dias, world!
```

File `example.stg`:
```
greet(hellos) ::= <<
<hellos: world()>
>>

world(item) ::= <<
<item>, world!

>>
```
## TODO
* Renderer support
* Ready-made renderers (common lisp)
