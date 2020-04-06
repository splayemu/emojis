(ns emojis.page
  (:require
    [clojure.java.io :as io]
    [hiccup.page :refer [html5]]
    [stasis.core :as stasis]
    [cheshire.core :as json]))

(defn index [{:keys [emojis-sources] :as data}]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Emojis"]
    [:link {:rel "stylesheet" :href "/styles/styles.css"}]
    [:script {:src "/js/app.js"
              :type "text/javascript"}]
    [:script (str "var emoji_data=" (json/encode data))]
    [:script "emojis.main.init(emoji_data);"]]
   [:body
    [:div.body "meow"]
    [:div#emoji_app]
    #_(into [:div]
          (map (fn [[name source]]
            [:div
             name
             [:img {:src source}]])
               emojis-sources)
          )]))
