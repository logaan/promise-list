(ns promise-list.dcell
  (:require [jayq.core :as jq]))

(defn deferred
  ([] (jq/$deferred))
  ([value] (jq/resolve (deferred) value)))

(defrecord DCell [deferred-wrapping-cell])

(defn closed-container [v]
  (DCell. (deferred v)))

(defn open-container []
  (DCell. (deferred)))

(defn empty-cell []
  (closed-container nil))

(defn open-cell [v]
  (closed-container (cons v (open-container))))

(defn closed-cell [v1 v2]
  (closed-container (cons v1 v2)))

(defn done [dcell callback]
  (jq/done (:deferred-wrapping-cell dcell) callback))

(defn resolve [dcell callback]
  (jq/resolve (:deferred-wrapping-cell dcell) callback))

(extend-type DCell
  ISeq
  (-first [dcell]
    (let [first-deferred (deferred)]
      (done dcell (fn [cell]
        (jq/resolve first-deferred (first cell))))
      first-deferred))
  (-rest [dcell]
    (let [rest-deferred (deferred)]
      (done dcell (fn [cell]
        (let [tail (rest cell)]
          (if (empty? tail)
            (jq/resolve rest-deferred nil)
            (done (rest cell) (fn [rest-cell]
                                (jq/resolve rest-deferred rest-cell)))))))
      (DCell. rest-deferred)))
  
  ISeqable
  (-seq [this] this))

(defn dapply [f]
  (fn [d]
    (let [new-d (jq/$deferred)]
      (jq/done d (fn [v]
        (jq/resolve new-d (f v))))
      new-d)))
