(ns promise-list.quick-search-test
  (:use [jayq.util :only [log]]
        [jayq.core :only [$ on val text remove append]]
        [promise-list.pcell :only [deferred]]
        [promise-list.plist :only [with-open-plist append! map* mapd* concat*]]))
(defn timestamp []
  (.valueOf (js/Date.)))

(defn metranome [interval]
  (with-open-plist (fn [writer]
    (js/window.setInterval
      #(append! writer (deferred {:time (timestamp)}))
      interval))))

(comment

(defn event-list [element event-type]
  (with-open-plist (fn [writer]
    (on element event-type (fn [event]
      (append! writer (deferred event))
      (.preventDefault event))))))

(defn summarise [event]
  (let [target (aget event "target")]
    {:type   (aget event "type")
     :time   (aget event "timeStamp")
     :target target
     :value  (aget target "value")}))

(defn transparent-log [v]
  (log (clj->js v))
  v)

(defn perform-search [query]
  (js/jQuery.getJSON (str "http://api.flickr.com/services/rest/?method=flickr.groups.search&api_key=f4640a7dc5acccbb86af84db4d311010&text=" query "&per_page=10&format=json&jsoncallback=?")))

(defn group-names [response]
  (if-let [groups (aget response "groups")]
    (map #(aget % "name") (aget groups "group"))))

(defn set-query-title! [new-title]
  (text ($ :#query-title) new-title))

(defn set-results-list! [results]
  (remove ($ "#results li"))
  (mapv #(append ($ :#results) (str "<li>" % "</li>")) results))

(comment
  (let [changes   (event-list ($ :#query) "change")
      keyups    (event-list ($ :#query) "keyup")
      events    (concat* changes keyups)
      queries   (mapd* (comp :value summarise) events)
      responses (map*  perform-search queries)
      groups    (mapd* group-names responses)]
  (mapd* set-query-title!  queries)
  (mapd* transparent-log events)
  (mapd* set-results-list! groups))))

(log (str "loaded at: " (timestamp)))
(js/window.setTimeout
 (fn []
   (log (str "starting at: " (timestamp)))
   (let [clock (metranome 200)]
     (mapd* identity clock)))
  (* 30 1000))

(comment (js/window.setInterval (fn [] (+ 1 1)) 200))

