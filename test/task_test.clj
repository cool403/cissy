(ns task-test
  (:require [clojure.test :as test]
            [cissy.task :as task]))

(test/deftest name-test
  (test/testing "Test if the configuration is ok"
    (test/is (= 2 2))))
