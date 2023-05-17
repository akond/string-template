(ns com.github.akond.string-template.core-test
	(:require
		[clojure.test :refer :all]
		[com.github.akond.string-template.core :as st])
	(:import
		(java.util Locale)))

; https://github.com/antlr/stringtemplate4/blob/master/doc/index.md

(deftest Creation
	(testing "Simple things"
		(is (= "AB" (-> (st/template "A<x>") (assoc :x "B") (str))))
		(is (= "ADEF:" (-> (st/template "A<x><y><iso_date>")
						   (merge {:x        "D"
								   :y        (st/template "EF")
								   :iso-date ":"})
						   str))))

	(testing "Anonymous template"
		(is (= "A1-211-22" (-> (st/template "A<list:{attr | <attr.x>-<attr.y>}>")
							   (merge {:list [{:x 1 :y 2} {:x 11 :y 22}]})
							   str))))

	(testing "Renderers"
		(is (= "cat......." (-> (st/group "string(s) ::= <<<s; format=\"~10,1,0,'.A\">\n>>" :renderers [[String st/cl-renderer]])
								:string
								(merge {:s "cat"})
								str)))
		(is (= "dog" (-> (st/group "string(s) ::= <<<s>\n>>" :renderers [st/cl-renderer])
						 :string
						 (merge {:s "dog"})
						 str)))
		(let [gr (-> (st/group "number(n) ::= <<<n; format=\"%+10.4f\">!>>" :renderers [st/java-renderer])
					 :number
					 (merge {:n Math/E}))]
			(is (= "   +2.7183!" (-> gr (st/render))))
			(is (= "   +2,7183!" (-> gr (st/render {:locale Locale/FRANCE}))))))

	(testing "Fixed width output"
		(let [string (repeat 4 "cat")
			  t      (-> (st/template "<s; wrap, separator=\" \">.") (merge {:s string}))]
			(is (= "cat cat cat \ncat." (st/render t {:line-width 10}))))))

(deftest Groups
	(testing "Template reference"
		(let [g (st/group "t5(list) ::= \"T5 <list:t6()>.\"
							t6(x) ::= \"T6 <x.a> <x.b>;\" ")]
			(is (= "T5 T6 123 4;T6 5 678;." (-> (get g :t5)
												(merge {:list [{:a 123 :b 4}
															   {:a 5 :b 678}]})
												str)))))

	(testing "Round robing"
		(let [g (st/group "t5(list) ::= \"<list:t6(), t7()>.\"
				t6(x) ::= \"T6\"
				t7(x) ::= \"T7\"")]
			(is (= "T6T7T6." (-> (get g :t5)
								 (merge {:list [1 2 3]})
								 str)))))

	(testing "External group"
		(let [g (st/group "t8(list) ::= <<+<list; separator=\",\">!>>")
			  t (st/with-group "S<t8(a)>" g)]
			(is (= "S+1,2,3!" (-> t (merge {:a [1 2 3]}) (str)))))))

(deftest ErrorCases
	(is (thrown? Throwable (str (st/template "<missing>")))))
