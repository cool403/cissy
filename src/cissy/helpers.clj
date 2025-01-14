(ns cissy.helpers)


(defn my-merge-fn [m1 m2]
  ;自定义合并，存在于m1,也存在于m2,保留m1；不存在于m1,存在于m2,添加
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          m1
          m2))