(ns com.github.akond.string-template.core
	(:import
		(clojure.lang Associative ILookup IPersistentCollection IPersistentMap)
		(java.io File)
		(org.stringtemplate.v4 ST STGroupFile STGroupString)))

; os described at https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
(def reserved (->>
				  '(true, false, import, default, key, group, implements, first, last, rest, trunc, strip, trim, length,
					   strlen, reverse, if, else, elseif, endif, delimiters)
				  (map str)
				  (into #{})))

(defn stringify [k]
	(cond-> k
		(or (keyword? k) (symbol? k)) name))

(deftype StringTemplate [template]
	Object
	(toString [_]
		(.render template))

	IPersistentCollection
	(cons [this a]
		(reduce (partial apply assoc) this a)
		this)

	(empty [this]
		(doseq [k (keys (.getAttributes template))]
			(.remove template k))
		this)

	IPersistentMap
	(without [this k]
		(.remove template (stringify k))
		this)

	Associative
	(assoc [this k v]
		(let [k      (stringify k)
			  adjust (some-fn
						 (fn [v]
							 (when (and (map? v) (not (instance? StringTemplate v)))
								 (update-keys v stringify)))
						 identity)]
			(when (contains? reserved k)
				(throw (IllegalArgumentException. (format "'%s' is a reserved name." k))))
			(.add template k (adjust v)))
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

(deftype StringTemplateGroup [g]
	ILookup
	(valAt [this k]
		(template (.getInstanceOf g (stringify k))))
	(valAt [this k none]
		(let [k' (stringify k)]
			(if (.isDefined g k')
				(template (.getInstanceOf g k'))
				none))))

(defmulti group class)

(defmethod group :default [s]
	(->StringTemplateGroup (STGroupString. s)))

(defmethod group File [s]
	(let [path (str (.getAbsoluteFile s))]
		(->StringTemplateGroup (STGroupFile. path))))
