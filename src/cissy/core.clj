(ns cissy.core
  (:require
    [cissy.const :as const]
    [cissy.executions :as executions]
    [cissy.registry :as register]
    [cissy.task :as task]
    [clojure.core.async :refer [>! alts! go timeout]]
    [clojure.string :as str]
    [taoensso.timbre :as timbre]))


; Fill execution parameters
(defn- fill-node-param [node-execution-info curr-node-id task-config]
  (let [node-rel-config ((keyword curr-node-id) task-config)
        db-keys (filter #(str/ends-with? % const/db-suffix) (keys node-rel-config))]
    ; Put all node-rel-config into node-param-dict
    (doseq [db-key db-keys]
      (let [db-ref-key (get node-rel-config db-key)
            db-ins (register/get-datasource-ins (keyword db-ref-key))]
        (timbre/info "node=" curr-node-id "dependent datasource configuration:" db-ref-key "add" db-ins)
        (-> (:node-param-dict @node-execution-info)
            (#(reset! % (assoc (deref %) (keyword db-ref-key) db-ins))))))
    (reset! (:node-param-dict @node-execution-info) (merge (deref (:node-param-dict @node-execution-info)) node-rel-config)))
  node-execution-info)

; Fill execution result set
(defn- fill-node-result-cxt [node-execution-info curr-node-id node-graph may-used-node-res]
  ; Get parent node list
  (when-let [parent-node-list (task/get-parent-nodes node-graph curr-node-id)]
    (doseq [parent-node parent-node-list]
      ; Pass the execution result of the parent node as the dependency for this node execution
      (let [parent-node-id (:node-id parent-node)
            node-result-dict (:node-result-dict @node-execution-info)]
        (timbre/info "node-id=" curr-node-id ", dependent parent-node" parent-node-id ",returns" (if (counted? may-used-node-res)
                                                                                    (str (count may-used-node-res) " records")
                                                                                    may-used-node-res))
        (reset! node-result-dict (assoc @node-execution-info (keyword parent-node-id) may-used-node-res)))))
  node-execution-info)

(defn- fill-thread-info [node-execution-info thread-idx round]
  ;; Fill thread information
  ;; thread-idx: thread index
  ;; round: execution round
  (let [node-execution-dict (:node-execution-dict @node-execution-info)]
    (reset! node-execution-dict (assoc @node-execution-dict :thread-idx thread-idx :execution-round round)))
  node-execution-info)

(defn- check-parent-nodes-done? [node-id node-graph task-execution-dict]
  ; Check if all parent nodes are done
  (let [parent-node-lst (task/get-parent-nodes node-graph node-id)
        parent-node-id-set (set (map :node-id parent-node-lst))]
    (if (and (not-empty parent-node-id-set)
             (every? #(= "done" (get @task-execution-dict (keyword %))) parent-node-id-set))
      true
      false)))

(defn- check-child-nodes-done? [node-id node-graph task-execution-dict]
  ; Check if all child nodes are done
  (let [child-node-lst (task/get-child-nodes node-graph node-id)
        child-node-id-set (set (map :node-id child-node-lst))]
    (if (and (not-empty child-node-id-set)
             (every? #(= "done" (get @task-execution-dict (keyword %))) child-node-id-set))
      true
      false)))

(defn- execute-node-fn [thread-node-execution node-func node-monitor-channel thread-idx]
  ;; Execute node-func, log error if execution fails, stop current thread, send channel
  (try
    (let [r (node-func thread-node-execution)
          node-execution-dict (:node-execution-dict @thread-node-execution)
          node-id (:node-id @thread-node-execution)]
      ; It is possible that the subtask has been completed, such as returning empty, this needs to be handled
      (if (= (get @node-execution-dict (keyword (str thread-idx))) "done")
        (do
          (go
            ;; Send message to chan must be in go block
            (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx}))
          [:fail nil])
        [:ok r]))
    (catch Exception e
      (timbre/error "Executing node nodeId=" (:node-id @thread-node-execution) "thread-idx=" thread-idx
                    "exception occurred, exception message:" (.getMessage e) e)
      (go
        ;; Send message to chan must be in go block
        (>! node-monitor-channel {:node-id    (:node-id @thread-node-execution) :node-status "done"
                                  :thread-idx thread-idx}))
      [:fail nil])))

(defn process-node-chan
  "Execute once"
  [node-id task-execution-info node-channels node-graph node-monitor-channel]
  (let [{task-info :task-info} @task-execution-info
        node-func (register/get-node-func node-id)
        node-chan (get @node-channels node-id)
        child-nodes (get (:child-node-map node-graph) node-id)
        child-chans (map #(get @node-channels (:node-id %)) child-nodes)
        thread-count (or (get-in @task-info [:task-config (keyword node-id) :threads]) 1)
        curr-offset (ref 0)
        ;; Only using atom cannot guarantee that the offset obtained in a multi-threaded situation is not duplicated;;stm
        ;; lock (ReentrantLock.)
        get-offset-fn (fn [page-size]
                        (dosync
                          (alter curr-offset + page-size)
                          @curr-offset))]

    ;; Create the specified number of worker threads
    (dotimes [thread-idx thread-count]
      (let [thread-node-execution (-> (executions/new-node-execution-info node-id task-execution-info)
                                      (fill-node-param node-id (:task-config @task-info)))
            task-execution-dict (:task-execution-dict @task-execution-info)]
        (go
          ; Set node status to ding
          (reset! thread-node-execution (assoc @thread-node-execution :curr-node-status "ding"))

          (when-not (= (:curr-task-status @task-execution-info) "done")
            (timbre/info (str "Create thread " thread-idx " for node " node-id ", execution round " 1))
            ;; Update execution information
            (let [curr-node-execution (-> thread-node-execution
                                          (fill-thread-info thread-idx 1))
                  curr-node-status (:curr-node-status @curr-node-execution)
                  ;; If there is a calc-page-offset function, calculate the new offset
                  node-param-dict (:node-param-dict @curr-node-execution)]
              (>! node-monitor-channel {:node-id    node-id :node-status curr-node-status
                                        :thread-idx thread-idx})
              ;; If there is an offset calculation function, update page_offset
              (when (contains? @node-param-dict :page_size)
                (let [page-size (get @node-param-dict :page_size 1000)]
                  (reset! node-param-dict
                          (assoc @node-param-dict :page_offset (get-offset-fn page-size)))
                  (timbre/info "Current thread-index=" thread-idx "obtained offset=" (get @node-param-dict :page_offset))))
              ;(prn curr-node-status)
              (when (not= curr-node-status "done")
                (if node-chan
                  ;; Non-root node waits for input or parent node is done, current node marked as done
                  ;; Wait for 3s
                  (let [time-out (timeout 3000)
                        ;{:priority true} If multiple channels have data at the same time, prioritize in order
                        ; Ensure that there will be no data loss or anything
                        [curr-result ch] (alts! [node-chan time-out] {:priority true})]
                    (if (= ch time-out)
                      ; Timeout to determine if the parent node is already done
                      (when (check-parent-nodes-done? node-id node-graph task-execution-dict)
                          (timbre/info (str "Worker node " node-id " thread-idx=" thread-idx "all parent nodes are done, current thread task status marked as done"))
                          ; Send done status
                          (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx}))
                      (do
                        (timbre/info (str "Node " node-id "obtained parent node result"))
                        (let [[status result] (-> curr-node-execution
                                                  (fill-node-result-cxt node-id node-graph curr-result)
                                                  (execute-node-fn node-func node-monitor-channel thread-idx))]
                          (when (= status :ok)
                            (doseq [ch child-chans]
                              (>! ch result))
                            (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx}))))))

                  ;; Root node execution
                  (do
                    (timbre/info (str "Start root node " node-id))
                    ;; If the direct child nodes of the root node are all done, the root node status is done and not executed
                    (if-not (check-child-nodes-done? node-id node-graph task-execution-dict)
                      (let [[status result] (-> curr-node-execution
                                                (execute-node-fn node-func node-monitor-channel thread-idx))]
                        (when (= status :ok)
                          (doseq [ch child-chans]
                            (>! ch result))
                          (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx})))
                      (do
                        (timbre/info (str "Root node nodeId=" node-id " all subtasks are done"))
                        (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx})))))))))))))


(defn process-node-chan-loop
  "Process node execution based on Channel"
  [node-id task-execution-info node-channels node-graph node-monitor-channel]
  (let [{task-info :task-info} @task-execution-info
        node-func (register/get-node-func node-id)
        node-chan (get @node-channels node-id)
        child-nodes (get (:child-node-map node-graph) node-id)
        child-chans (map #(get @node-channels (:node-id %)) child-nodes)
        thread-count (or (get-in @task-info [:task-config (keyword node-id) :threads]) 1)
        curr-offset (ref 0)
        ;; Only using atom cannot guarantee that the offset obtained in a multi-threaded situation is not duplicated;;stm
        ;; lock (ReentrantLock.)
        get-offset-fn (fn [page-size]
                        (dosync
                          (alter curr-offset + page-size)
                          @curr-offset))]

    ;; Create the specified number of worker threads
    (dotimes [thread-idx thread-count]
      (let [thread-node-execution (-> (executions/new-node-execution-info node-id task-execution-info)
                                      (fill-node-param node-id (:task-config @task-info)))
            task-execution-dict (:task-execution-dict @task-execution-info)]
        (go
          ; Set node status to ding
          (reset! thread-node-execution (assoc @thread-node-execution :curr-node-status "ding"))
          (loop [round 1]
            (when-not (= (:curr-task-status @task-execution-info) "done")
              (timbre/info (str "Create thread " thread-idx " for node " node-id ", execution round " round))
              ;; Update execution information
              (let [curr-node-execution (-> thread-node-execution
                                            (fill-thread-info thread-idx round))
                    curr-node-status (:curr-node-status @curr-node-execution)
                    ;; If there is a calc-page-offset function, calculate the new offset
                    node-param-dict (:node-param-dict @curr-node-execution)]
                (>! node-monitor-channel {:node-id    node-id :node-status curr-node-status
                                          :thread-idx thread-idx})
                ;; If there is an offset calculation function, update page_offset
                (when (contains? @node-param-dict :page_size)
                  (let [page-size (get @node-param-dict :page_size 1000)]
                    (reset! node-param-dict
                            (assoc @node-param-dict :page_offset (get-offset-fn page-size)))
                    (timbre/info "Current thread-index=" thread-idx "obtained offset=" (get @node-param-dict :page_offset))))
                ;(prn curr-node-status)
                (when (not= curr-node-status "done")
                  (if node-chan
                    ;; Non-root node waits for input or parent node is done, current node marked as done
                    ;; Wait for 3s
                    (let [time-out (timeout 3000)
                          ;{:priority true} If multiple channels have data at the same time, prioritize in order
                          ; Ensure that there will be no data loss or anything
                          [curr-result ch] (alts! [node-chan time-out] {:priority true})]
                      (if (= ch time-out)
                        ; Timeout to determine if the parent node is already done
                        (if (check-parent-nodes-done? node-id node-graph task-execution-dict)
                          (do
                            (timbre/info (str "Worker node " node-id " thread-idx=" thread-idx "all parent nodes are done, current thread task status marked as done"))
                            ; Send done status
                            (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx}))
                          (recur round))
                        (do
                          (timbre/info (str "Node " node-id "obtained parent node result"))
                          (let [[status result] (-> curr-node-execution
                                                    (fill-node-result-cxt node-id node-graph curr-result)
                                                    (execute-node-fn node-func node-monitor-channel thread-idx))]
                            (when (= status :ok)
                              (doseq [ch child-chans]
                                (>! ch result))
                              (recur (inc round)))))))
                    ;; Root node execution
                    (do
                      (timbre/info (str "Start root node " node-id))
                      ;; If the direct child nodes of the root node are all done, the root node status is done and not executed
                      (if-not (check-child-nodes-done? node-id node-graph task-execution-dict)
                        (let [[status result] (-> curr-node-execution
                                                  (execute-node-fn node-func node-monitor-channel thread-idx))]
                          (when (= status :ok)
                            (doseq [ch child-chans]
                              (>! ch result))
                            (recur (inc round))))
                        (do
                          (timbre/info (str "Root node nodeId=" node-id " all subtasks are done"))
                          (>! node-monitor-channel {:node-id node-id :node-status "done" :thread-idx thread-idx}))))))))))))))
