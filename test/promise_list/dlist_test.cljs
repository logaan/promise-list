(ns promise-list.dlist-test
  (:use [promise-list.dlist :only [closed-dlist open-dlist append! close! reduce*]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]
            [promise-list.dcell :as dc]))

; dlist
(jq/done (first (rest (closed-dlist 1 2 3))) #(assert (= 2 %)))

; append!
(let [[reader writer] (open-dlist)]
  (jq/done (first reader) #(assert (= 1 %)))
  (jq/done (first (rest reader)) #(assert (= 2 %)))
  (jq/done (first (rest (rest reader))) #(assert  (= 3 %)))
  (jq/done (first (rest (rest (rest reader)))) #(assert  (= 4 %)))
  (reduce append! writer (map dc/deferred [1 2 3 4])))

; Should output 1\n2

; close!
(let [[reader writer] (open-dlist)]
  (dc/done reader #(assert (empty? %)))
  (close! writer))

; HOFs
(->> (closed-dlist 1 2 3)
     (map (dc/dapply inc))
     (map (dc/dapply #(assert (#{2 3 4} %))))
     (take 3)
     doall)

(jq/done (reduce* + 0 (closed-dlist 1 2)) #(assert (= 3 %)))

