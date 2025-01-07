(ns task-test
    (:require [clojure.test  :as test]
              [cissy.task :as task]))


(test/deftest name-test
      (test/testing "Context of the test assertions"
        (test/is (= 2 2)))) 
