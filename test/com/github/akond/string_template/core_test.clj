(ns com.github.akond.string-template.core-test
	(:require
		[clojure.test :refer :all]
		[com.github.akond.string-template.core :as st]))

(deftest Creation
	(is (= "AB" (-> (st/template "A<x>") (assoc :x "B") (str))))
	(is (= "AC" (-> (st/template "A<x>") (merge {:x "C"}) (str))))
	(is (= "AD\n\tEF" (-> (st/template "A<x>\n\t<y>")
						  (merge {:x "D"
								  :y (st/template "EF")})
						  str))))
