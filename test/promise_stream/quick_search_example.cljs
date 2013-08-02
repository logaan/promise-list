(ns promise-stream.quick-search-test
  (:require [jayq.core :as jq]) 
  (:use [jayq.core            :only [$]]
        [promise-stream.sources :only [event-stream]]
        [promise-stream.pstream   :only [mapd* concat* throttle*]]
        [promise-stream.timing-aware :only
         [resolve-order-map* keep-most-recently-requested
          stamp-with-request-time]]))

(defn perform-search [query]
  (js/jQuery.getJSON
    (str "http://api.flickr.com/services/rest/"
         "?method="       "flickr.groups.search"
         "&api_key="      "10b278da620908b32d4cb5e044366699"
         "&text="         query
         "&per_page="     10
         "&format="       "json"
         "&jsoncallback=" "?")))

(defn group-names [response]
  (if-let [groups (aget response "groups")]
    (for [group (aget groups "group")]
      (aget group "name"))))

(defn set-query-title! [new-title]
  (jq/text ($ :#query-title) new-title))

(defn set-results-stream! [results]
  (jq/remove ($ "#results li"))
  (mapv #(jq/append ($ :#results) (str "<li>" % "</li>")) results))

((fn []
  (let [changes   (event-stream ($ :#query) "change")
        keyups    (event-stream ($ :#query) "keyup")
        events    (throttle* 400 (concat* changes keyups))
        queries   (mapd* #(.-value (.-target %)) events)
        responses (resolve-order-map* 
                    (stamp-with-request-time perform-search) queries)
        filtered  (keep-most-recently-requested responses)
        groups    (mapd* group-names filtered)]
    (mapd* set-query-title!  queries)
    (mapd* set-results-stream! groups))))

(defn update-latest-result [response]
  (jq/text ($ :#latest_result) response))

((fn []
   (let [slows     (event-stream ($ :#slow) "click")
         fasts     (event-stream ($ :#fast) "click")
         events    (concat* slows fasts)
         endpoints (mapd* #(str "/" (.-id (.-currentTarget %))) events)
         responses (resolve-order-map* (stamp-with-request-time js/jQuery.get) endpoints)
         mrr       (keep-most-recently-requested responses)]
   (mapd* update-latest-result mrr))))

