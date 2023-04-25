(ns com.github.akond.string-template.core
	(:import
		(clojure.lang Associative ILookup IPersistentCollection IPersistentMap)
		(java.io File)
		(org.stringtemplate.v4 ST STGroupFile STGroupString)))

; https://www.stringtemplate.org/
; https://github.com/antlr/stringtemplate4/blob/master/doc/index.md

; os described at https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
(def reserved (->>
				  '(true, false, import, default, key, group, implements, first, last, rest, trunc, strip, trim, length,
					   strlen, reverse, if, else, elseif, endif, delimiters)
				  (map str)
				  (into #{})))

(defn stringify [k]
	(cond-> k
		(or (keyword? k) (symbol? k)) name))

(defprotocol IRaw
	(raw [this]))

(deftype StringTemplate [string-template]
	IRaw
	(raw [this]
		string-template)

	Object
	(toString [_]
		(.render string-template))

	IPersistentCollection
	(cons [this a]
		(reduce (partial apply assoc) this a)
		this)

	(seq [this]
		())

	(empty [this]
		(doseq [k (keys (.getAttributes string-template))]
			(.remove string-template k))
		this)


	IPersistentMap
	(without [this k]
		(.remove string-template (stringify k))
		this)

	Associative
	(assoc [this k v]
		(let [k      (stringify k)
			  adjust (some-fn
						 (fn [v]
							 (when (instance? StringTemplate v)
								 (.string_template v)))
						 (fn [v]
							 (when (and (map? v) (not (instance? StringTemplate v)))
								 (update-keys v stringify)))
						 identity)]
			(when (contains? reserved k)
				(throw (IllegalArgumentException. (format "'%s' is a reserved name." k))))
			(.add string-template k (clojure.walk/postwalk adjust v)))
		this))

(defmulti template class)

(defmethod template :default [s]
	(->StringTemplate (ST. s)))

(defmethod template ST [s]
	(->StringTemplate s))

(defmethod template File [s]
	(->StringTemplate (ST. (slurp s))))

(defmethod print-method StringTemplate [t w]
	(->> t str (.write w)))

(deftype StringTemplateGroup [template-group]
	IRaw
	(raw [_]
		template-group)
	ILookup
	(valAt [this k]
		(template (.getInstanceOf template-group (stringify k))))
	(valAt [this k none]
		(let [k' (stringify k)]
			(if (.isDefined template-group k')
				(template (.getInstanceOf template-group k'))
				none))))

(defmulti group class)

(defmethod group :default [s]
	(->StringTemplateGroup (STGroupString. s)))

(defmethod group File [s]
	(let [path (str (.getAbsoluteFile s))]
		(->StringTemplateGroup (STGroupFile. path))))
