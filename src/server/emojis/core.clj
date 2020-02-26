(ns emojis.core
  (:require
    [emojis.page :as page]
    [emojis.slack-messages :as slack-messages]
    [shadow.cljs.devtools.api :as shadow]
    [clojure.java.io :as io]
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [org.httpkit.server :as server]
    [aero.core :as aero]
    [stasis.core :as stasis]))

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
  (reset! running-server (server/run-server app {:port 8085})))

(defn stop-serving-assets! []
  (when @running-server
    (@running-server))
  (reset! running-server nil))

(defn serve-development []
  (serve-assets!))

(comment
  (let [api-token (:api-token (aero/read-config (io/resource "config/private.edn")))]
    (reset! emojis-sources (get-custom-emojis api-token)))

  (let [slack-export-directory-name "resources/slack_export"]
    (reset! emojis-summary (slack-messages/inport-emojis slack-export-directory-name))
    :ok)

  (serve-development)

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
