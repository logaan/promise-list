(ns promise-list.dcell
  (:require [jayq.core :as jq]))

(defn deferred [value]
  (jq/resolve (jq/$deferred) value))

(deftype DCell [deferred-wrapping-cell])

(defn closed-container [v]
  (DCell. (deferred v)))

(defn open-container []
  (DCell. (jq/$deferred)))

(defn empty-cell []
  (closed-container nil))

(defn open-cell [v]
  (closed-container (cons v (open-container))))

(defn closed-cell [v1 v2]
  (closed-container (cons v1 v2)))

(defn done [dcell callback]
  (jq/done (.-deferred-wrapping-cell dcell) callback))

(defn resolve [dcell value]
  (jq/resolve (.-deferred-wrapping-cell dcell) value))

(extend-type DCell
  ISeq
  (-first [dcell]
    (let [first-deferred (jq/$deferred)]
      (done dcell (fn [cell]
        (jq/resolve first-deferred (first cell))))
      (jq/promise first-deferred)))
  (-rest [dcell]
    (let [rest-deferred (jq/$deferred)]
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
  (fn
    ([]
     (deferred (f)))
    ([d]
     (let [new-d (jq/$deferred)]
       (jq/done d (fn [v]
         (jq/resolve new-d (f v))))
       new-d))
    ([d1 d2]
     (let [new-d (jq/$deferred)]
       (jq/done d1 (fn [v1]
         (jq/done d2 (fn [v2]
           (jq/resolve new-d (f v1 v2))))))
       new-d))))

