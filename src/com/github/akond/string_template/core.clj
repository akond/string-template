(ns com.github.akond.string-template.core
	(:require
		[clojure.pprint :refer [cl-format]])
	(:import
		(clojure.lang Associative ILookup IPersistentCollection IPersistentMap)
		(java.io File StringWriter Writer)
		(java.util Locale)
		(org.stringtemplate.v4 AttributeRenderer AutoIndentWriter ST STGroup STGroupFile STGroupString STWriter)
		(org.stringtemplate.v4.misc ErrorBuffer STMessage)))

; https://www.stringtemplate.org/
; https://github.com/antlr/stringtemplate4/blob/master/doc/index.md

; os described at https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
(def reserved (->>
				  '(true, false, import, default, key, group, implements, first, last, rest, trunc, strip, trim, length,
					   strlen, reverse, if, else, elseif, endif, delimiters)
				  (map str)
				  (into #{})))

(defn stringify [k]
	(munge
		(cond-> k
			(or (keyword? k) (symbol? k)) name)))
(defprotocol IRaw
	(raw [this]))

(defprotocol IStringTemplate
	(render [this] [this opts]))

(deftype StringTemplate [^ST string-template]
	IRaw
	(raw [this]
		string-template)

	IStringTemplate
	(render [this] (render this nil))
	(render [_ {:keys [^Long line-width ^Locale locale]
				:or   {^Long line-width STWriter/NO_WRAP
					   ^Locale locale   (Locale/getDefault)}}]
		(let [^StringWriter string-writer (StringWriter.)
			  ^ErrorBuffer error-buffer   (ErrorBuffer.)
			  auto-indent-writer          (doto (AutoIndentWriter. string-writer)
											  (.setLineWidth line-width))]
			(.write string-template auto-indent-writer locale error-buffer)
			(when-let [errors (seq (.-errors error-buffer))]
				(throw (Exception. ^String (.toString ^STMessage (first errors)))))

			(str string-writer)))

	Object
	(toString [this]
		(render this))

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
								 (.string_template ^StringTemplate v)))
						 (fn [v]
							 (when (and (map? v) (not (instance? StringTemplate v)))
								 (update-keys v stringify)))
						 identity)]
			(when (contains? reserved k)
				(throw (IllegalArgumentException. (format "'%s' is a reserved name." k))))
			(.add string-template k (clojure.walk/postwalk adjust v)))
		this))

(defmulti template (fn [o & _] (class o)))

(defmethod template :default [^String s]
	(->StringTemplate (ST. s)))

(defmethod template ST [^ST s]
	(->StringTemplate s))

(defmethod template com.github.akond.string_template.core.StringTemplate [s]
	s)

(defmethod template File [^File s]
	(->StringTemplate (ST. (slurp s))))

(defmethod print-method StringTemplate [t ^Writer w]
	(->> t str (.write w)))

(deftype StringTemplateGroup [^STGroup template-group]
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

(defn ^AttributeRenderer create-renderer [f]
	(reify AttributeRenderer
		(toString [_ val format locale]
			(if format
				(f val format locale)
				(str val)))))

(defn instrument-group [^STGroup g opts]
	(let [{:keys [renderers]} opts
		  renderers (if (fn? renderers) [[Object renderers]] renderers)]
		(doseq [renderer (seq renderers)
				:let [[^Class type renderer] (if (fn? renderer)
												 [Object renderer]
												 renderer)]]
			(-> g (.registerRenderer type
					  (create-renderer renderer))))

		(->StringTemplateGroup g)))

(defmulti group (fn [o & _] (class o)))

(defmethod group :default [s]
	(throw (IllegalArgumentException. (format "Type %s is unexpected." (-> s class .getName)))))

(defmethod group String [s & opts]
	(instrument-group (STGroupString. s) opts))

(defmethod group File [^File s & opts]
	(let [path (str (.getAbsoluteFile s))]
		(instrument-group (STGroupFile. path) opts)))

(defn with-group [s g]
	(let [s (template s)]
		(set! (. ^ST (raw s) groupThatCreatedThisInstance) (raw g))
		s))

(defn cl-renderer [val fmt locale]
	(cl-format nil fmt val))

(defn java-renderer [val fmt locale]
	(String/format locale fmt (to-array [val])))
