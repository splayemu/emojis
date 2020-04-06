(ns emojis.slack-messages
  (:require
    [clojure.java.io :as io]
    [me.raynes.fs :as fs]
    [cheshire.core :as json]))

(defn count-reactions [message]
  (->> (get message :reactions)
    (map (fn [reaction]
              [(get reaction :name) (get reaction :count)]))
    (into {})))

(def emoji-re #":([a-z0-9_-]+(::skin-tone-[0-9]+)?):")

(defn count-text-emojis [message]
  (when (:text message)
    (->> (re-seq emoji-re (:text message))
      (map second)
      frequencies)))

(defn count-emojis [message]
  (merge-with +
    (count-reactions message)
    (count-text-emojis message)))

(comment
  (count-emojis
   {:text "amazzing idea andrew. :bell: I'd do :damon: that. :damon"
    :reactions [{:name "take_my_money",
                 :users
                 ["UAUCN8FD4"
                  "UN83F74DN"
                  "U3WGWKQRK"
                  "U0AF0N7GX"],
                 :count 4}
                {:name "bell",
                 :users
                 ["U9PT2E0E8"
                  "U3WGWKQRK"
                  "U0AF0N7GX"
                  "UAUCN8FD4"],
                 :count 4}]})

  ;; another message
  {:type "message",
   :text "I might actually head to Mixt Greens as soon as I push this fix",
   :user "U1ZRZ8T7F",
   :ts "1396291398.000074",
   :team "T02T3ELUQ",
   :user_team "T02T3ELUQ",
   :source_team "T02T3ELUQ",
   :user_profile
   {:first_name "Meow",
    :real_name "Meow",
    :name "Meow",
    :is_restricted false,
    :image_72
    "https://secure.gravatar.com/avatar/204ee6169730e6b7529817e8bda546c3.jpg?s=72&d=https%3A%2F%2Fa.slack-edge.com%2Fdf10d%2Fimg%2Favatars%2Fava_0012-72.png",
    :avatar_hash "g04ee6169730",
    :is_ultra_restricted false,
    :team "T02T3ELUQ",
    :display_name "eric"}}

  )

(defn parse-messages [day-file]
  (with-open [in-file (io/reader day-file)]
    (let [messages (json/parse-stream in-file true)]
      (def tn messages)
      (->> messages
           (map count-emojis)
           (reduce (partial merge-with +))))))

(defn summarize [emojis-list]
  (->> emojis-list
    (map :emojis)
    (reduce (partial merge-with +))))

(defn summarize-emojis [emojis-list]
  (let [daily-emojis   (->> (group-by :date emojis-list)
                         (map (juxt key (comp summarize val)))
                         (into (sorted-map))

                         #_(reduce (partial merge-with +)))
        channel-emojis (->> (group-by :channel-name emojis-list)
                         (map (juxt key (comp summarize val)))
                         (into (sorted-map))
                         #_(map summarize)
                         #_(reduce (partial merge-with +)))
        total-emojis   (summarize emojis-list)]
    {:daily-emojis   daily-emojis
     :channel-emojis channel-emojis
     :total-emojis   total-emojis}))

(defn read-emojis
  "Reads the slack messages export files into a list of unique date-channel->emoji histogram maps.

  The slack messages export consists of directories corresponding to channels with date json files
  inside of them. E.G.

  slack_export/
    general_nonsense
      2020-01-01.json
      2020-01-02.json"
  [directory-name & [filter-emojis]]
  (let [root-directory (io/file directory-name)]
    (->> directory-name
         (fs/walk
          (fn [root dirs files]
            ;; Since we know the file heirarchy is only one level deep, we can walk along
            ;; each of the directories and process each of their files.

            ;; We skip the root directory.
            (when (fs/child-of? root-directory root)
              (let [channel-name (fs/name root)]
                (map (fn [date-filename]
                       (let [date-file (io/file directory-name
                                                (fs/name root)
                                                date-filename)
                             emojis    (parse-messages date-file)]
                         {:date         (fs/name date-file)
                          :channel-name channel-name
                          :emojis       (if filter-emojis
                                          (select-keys emojis filter-emojis)
                                          emojis)}))
                     files)))))
         (keep identity)
         (mapcat identity))))

(defn inport-emojis [directory-name]
  (-> directory-name
      read-emojis
      summarize-emojis))

(comment

  (def emojis (read-emojis "resources/test_export"))

  (def summary (summarize-emojis emojis))

  (->> (:total-emojis summary)
    (sort-by second)
    reverse
     )

  (take 20 (reverse (sort-by val (:total-emojis summary))))

  (take 10 (drop 2000 (:daily-emojis summary)))

  (group-by :a [{:a :fun} {:a :bun}])

  )
