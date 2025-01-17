(ns task-test
  (:require [clojure.test :as test]
            [cissy.task :as task]))


(test/deftest name-test
  (test/testing "试验测试配置是否ok"
    (test/is (= 2 2))))
