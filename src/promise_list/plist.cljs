(ns promise-list.plist
  (:use [jayq.util :only [log]])
  (:require [promise-list.pcell :as pc]
            [jayq.core :as jq]))

(defn closed-plist [& values]
  (reduce #(pc/closed-cell %2 %1) (pc/empty-cell) (reverse values)))

(defn reduce* [deferred coll f daccumulator]
  (let [dresult (f daccumulator (first coll))
        dtail   (rest coll)]
    (pc/done dtail (fn [tail]
      (if (empty? tail)
        (jq/done dresult (fn [result]
          (jq/resolve deferred result)))
        (reduce* deferred dtail f dresult))))))

(extend-type pc/DCell
  IReduce
  (-reduce
    ([coll f]
     (-reduce (rest coll) f (first coll)))
    ([coll f start]
     (let [response (jq/$deferred)]
       (reduce* response coll f start)
       response))))

(defn open-plist [& values]
  (let [tail  (pc/open-container)
        plist (reduce #(pc/closed-cell %2 %1) tail values)]
    (list plist (atom tail))))

(defn append! [writer dvalue]
  (let [current-tail @writer
        new-tail     (pc/open-container)]
    (jq/done dvalue (fn [value]
                      (let [contents (cons value new-tail)]
                        (pc/resolve current-tail contents))))
    (reset! writer new-tail)
    writer))

(defn close! [writer]
  (pc/resolve (deref writer) nil))

(defn traverse
  "vf will be called with every value. ef will be called at the end of the
  list."
  [coll vf ef]
   (pc/done coll (fn [cell]
     (if (empty? cell)
       (ef)
       (do 
         (vf (first cell))
         (traverse (rest cell) vf ef))))))

(defn map* [f coll]
   (let [[reader writer] (open-plist)]
     (traverse coll #(append! writer (f %)) #(close! writer))
     reader))

(defn copy-to-shared-collection [coll writer open-colls]
  (traverse coll
            #(append! writer (pc/deferred %))
            (fn []
              (swap! open-colls dec)
              (if (zero? @open-colls) (close! writer)))))

(defn concat* [& colls]
  (let [open-colls      (atom (count colls))
        [reader writer] (open-plist)]
    (doall (map #(copy-to-shared-collection % writer open-colls) colls))
    reader))

(defn count* [coll]
  (reduce (pc/dapply (fn [tally v] (inc tally))) (pc/deferred 0) coll))

; (defn mapcat* [f coll]
;   (let [total-colls     (atom nil)
;         completed-colls (atom 0)]
;     (jq/done (reduce (pc/dapply (fn [a v] (inc a))) coll)
;              (fn [count] ))))

