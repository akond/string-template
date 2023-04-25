(ns com.github.akond.string-template.core-test
	(:require
		[clojure.test :refer :all]
		[com.github.akond.string-template.core :as st]))

; https://github.com/antlr/stringtemplate4/blob/master/doc/index.md

(deftest Creation
	(is (= "AB" (-> (st/template "A<x>") (assoc :x "B") (str))))
	(is (= "ADEF" (-> (st/template "A<x><y>")
					  (merge {:x "D"
							  :y (st/template "EF")})
					  str)))

	(testing "Anonymous template"
		(is (= "A1-211-22" (-> (st/template "A<list:{attr | <attr.x>-<attr.y>}>")
							   (merge {:list [{:x 1 :y 2} {:x 11 :y 22}]})
							   str)))))

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
								 str))))))
