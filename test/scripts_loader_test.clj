(ns scripts-loader-test
  (:require [clojure.test :refer :all]
            [cissy.scripts-loader :as csl]))


(deftest load-deps
  (testing "测试deps加载"
    (is (csl/load-zip! "/home/mawdx/Desktop/hello.zip"))))
