(ns scripts-loader-test
  (:require [clojure.test :refer :all]
            [cissy.scripts-loader :as csl]))

(deftest load-deps
  (testing "Test deps loading"
    (csl/load-zip! "/home/mawdx/桌面/hello.zip"))
  (is (true? true)))
