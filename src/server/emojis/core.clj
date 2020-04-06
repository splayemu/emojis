(ns emojis.core
  (:require
    [emojis.page :as page]
    [emojis.slack-messages :as slack-messages]
    [emojis.summarize :as summarize]
    [shadow.cljs.devtools.api :as shadow]
    [clojure.java.io :as io]
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [org.httpkit.server :as server]
    [aero.core :as aero]
    [stasis.core :as stasis]
    [tick.alpha.api :as t]))

(defonce emojis-sources (atom {}))
(defonce emojis-summary (atom {}))

(def slack-directory "resources/slack_export")

#_(def slack-directory-abs-path
  (str (fs/absolute (io/file slack-directory))))

(defn get-pages []
  (merge (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
         {"/index.html" (page/index {:emojis-summary @emojis-summary
                                     :emojis-sources @emojis-sources})}))

(def app (stasis/serve-pages get-pages))

(defn get-custom-emojis [api-token]
  (let [url     "https://slack.com/api/emoji.list"
        headers {"Content-Type"  "application/x-www-form-urlencoded"
                 "Authorization" (str "Bearer " api-token)}

        {:keys [status headers body error] :as resp} @(http/get url {:headers headers})]
    (if (= status 200)
      (get (json/decode body) "emoji"))))

(def running-server (atom nil))
(defn serve-assets! []
  (reset! running-server (server/run-server app {:port 9085})))

(defn stop-serving-assets! []
  (when @running-server
    (@running-server)))

(defn serve-development []
  (serve-assets!))

;; TODO - come up with release flow
;; need to compile asssets
;; TODO - document dev and release flow commands
;; TODO - upload other sprite data
;; https://github.com/iamcal/emoji-data

(defn top-n-emojis [n]
  (->> @emojis-summary
       :total-emojis
       (sort-by val)
       reverse
       (take n)
       (map key)
       (into #{})))

(defn read-timeseries-emojis []
  (map (fn [[k v]]
         (let [[year month day] (clojure.string/split k #"-")]
           {:date   k
            :year   year
            :month  month
            :day    day
            :emojis v}))
       (:daily-emojis @emojis-summary)))

(defn filter-timeseries-emojis [timeseries-emojis]
  (let [top-100-emojis (top-n-emojis 100)]
    (map #(update % :emojis select-keys top-100-emojis) timeseries-emojis)))

(defn gather-bar-chart-race-data! [interval interpolation-k max-rank]
  (let [timeseries-emojis      (->> (read-timeseries-emojis)
                                    filter-timeseries-emojis
                                    (summarize/roll-up-by-interval interval)
                                    summarize/running-total
                                    :timeseries-emojis)
        keyframes              (summarize/keyframes interpolation-k max-rank timeseries-emojis)
        {:keys [prev next]}    (summarize/keyframe-prev+next keyframes)
        bar-chart-race-keyword (keyword (str "bar-chart-race-" (name interval)))]
    (swap! emojis-summary assoc bar-chart-race-keyword
           {:keyframes keyframes
            :prev      prev
            :next      next})
    :ok))

(comment
  (let [api-token (:api-token (aero/read-config (io/resource "config/private.edn")))]
    (reset! emojis-sources (get-custom-emojis api-token)))

  (let [slack-export-directory-name "resources/slack_export"]
    (reset! emojis-summary (slack-messages/inport-emojis slack-export-directory-name))
    :ok)

  (serve-development)

  ;; ---- Questions I want to answer?  ----
  ;; What are the most popular emojis in total?
  (reverse (sort-by val (:total-emojis @emojis-summary)))

  ;; What are the most popular emojis by day?
  (into [] (:daily-emojis @emojis-summary))

  ;; can we aggregate them by month? by year?
  (gather-bar-chart-race-data! :year 12 12)

  (keys @emojis-summary)

  (->> (:bar-chart-race-year @emojis-summary)
       :keyframes
       (drop 10)
       (take 10)
       )

  ;; compute the rank
  ;; compute the key values

  ;;process-emojis (comp (partial take 10) reverse (partial sort-by val) slack-messages/summarize val)
  ;;(map (juxt key process-emojis))

  (= (:total-emojis @emojis-summary) (:running-total rtotals))

  (:timeseries-emojis rtotals)

  ;; What are the most popular emojis by channel?
  (:channel-emojis @emojis-summary)

  ;; Who created the most popular emojis?
  ;; This one needs to read from: https://adstage.slack.com/customize/emoji as it's not supported by the api

  ;; What emojis does each person use the most?

  ;; Which person created the most popluar threads?

  ;; Which person has recieved the most emoijis?

  ;; what post got the most number of emojis?

  ;; what post got the most distinct emojis?

  ;; what thread had the most comments?

  ;; what thread had the most emojis?

  )

(defn export []
  (let [target-dir "resources/release"
        pages      (get-pages)]
    ;; first build release version of javascript
    (shadow/release :main)
    (stasis/empty-directory! target-dir)
    (stasis/export-pages pages target-dir)
    :exported))

(comment
  (export)

  )
