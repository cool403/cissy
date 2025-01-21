(ns task-config-edn-test
  (:require [clojure.test :as test]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))


(def config-path "task_config.edn" )

(test/deftest task-config-edn-test
  (test/testing "测试edn格式配置加载"
    (let [cfg (edn/read-string (slurp (io/resource config-path)))]
      (println cfg)
      (println (type cfg))
      (println (first (:tasks cfg)))
      (println (prn-str cfg))
      (test/is (not (nil? cfg))))))
