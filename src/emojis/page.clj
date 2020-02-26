(ns emojis.page
  (:require
    [clojure.java.io :as io]
    [hiccup.page :refer [html5]]
    [stasis.core :as stasis]
    [cheshire.core :as json]))

(defn index [data]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Emojis"]
    [:link {:rel "stylesheet" :href "/styles/styles.css"}]
    [:script (json/encode data)]]
   [:body
    [:div.body "meow"]]))
