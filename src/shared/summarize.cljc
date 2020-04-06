(ns emojis.summarize
  (:require
    [tick.alpha.api :as t]))

;; TODO: dump with slack-messages/summarize
(defn fold-emojis-list [emojis-list]
  (->> emojis-list
       (map :emojis)
       (reduce (partial merge-with +))))

(defn roll-up
  "Summarizes the emojis based on a time interval.

  Time intervals can be either :month or :year"
  [[date timeseries-emojis] interval]
  (if-let [{:keys [year month]} (first timeseries-emojis)]
    (if (= interval :month)
      {:emojis (fold-emojis-list timeseries-emojis)
       :year year
       :month month}
      {:emojis (fold-emojis-list timeseries-emojis)
       :year year})))

(defn roll-up-by-interval
  "Summarizes emojis by date interval.

  interval can be :month or :year.

  timeseries-emojis is of shape:
    [
      {:month  \"01
       :year   \"2019\"
       :day    \"01\"
       :emojis {\"bulb\" 1
                ...
                }}
    ]"
  [interval timeseries-emojis]
  (let [grouping-interval (if (= interval :month)
                            (juxt :year :month)
                            :year)]
    (->> timeseries-emojis
         (group-by grouping-interval)
         (map #(roll-up % interval))
         (sort-by grouping-interval)
         (map (fn [timeseries-emoji]
                (assoc timeseries-emoji
                       :date (str (:year timeseries-emoji)
                                  "-"
                                  (or (:month timeseries-emoji) "01")
                                  "-"
                                  "01")))))))

(defn running-total
  "Calculates the running total for a series of timeseries emojis."
  ([emojis] (running-total {} emojis))
  ([running-total emojis]
   (reduce (fn [r timeseries-emojis]
             (let [updated-timeseries-emojis
                   (update timeseries-emojis :emojis
                           #(merge-with + % (:running-total r)))]
               (-> r
                   (assoc :running-total (:emojis updated-timeseries-emojis))
                   (update :timeseries-emojis conj updated-timeseries-emojis))))
           {:running-total running-total
            :timeseries-emojis []}
           emojis)))

(defn rank
  "Calculates the zero based rank of the emojis.

  value-fn is a function that gets passed the emoji name and should return
  the value."
  [value-fn max-rank emojis]
  (->> emojis
       (map (fn [[emoji-name value]]
              {:name emoji-name
               ;; consider rounding this value
               :value (value-fn emoji-name)}))
       (sort-by :value)
       reverse
       (map-indexed (fn [index emoji]
                      (assoc emoji :rank (min index max-rank))))))

(partition 2 1 [:a :b :c])


(defn merge-default
  "Merges the keys of h2 with the value of default-value into h1."
  [h1 h2 default-value]
  (let [default-map (->> (interleave (keys h2) (repeat default-value))
                         (apply hash-map))]
    (merge default-map h1)))

(comment
  (merge-default {:a 1 :b 2} {:b 3 :c 1} 0)

  )

(defn date->ms [date]
  (.getTime (t/inst (t/at (t/date date) "00:00"))))

(defn ms->date [ms]
  (t/instant (t/new-duration ms :millis)))

(defn interpolate-keyframe-pair
  [{a-date :date a-emojis :emojis}
   {b-date :date b-emojis :emojis}
   interpolation-k
   max-rank]
  (let [;; add any emojis to a that b has with 0
        starting-emojis (merge-default a-emojis b-emojis 0)]
    ;; need to interpolate over the values and dates, and resort the order
    (for [i (range interpolation-k)]
      (let [t    (/ i interpolation-k)]
        {:date (str (ms->date (+ (* (date->ms a-date) (- 1 t)) (* (date->ms b-date) t))))
         :rank (rank #(+ (* (get a-emojis % 0) (- 1 t))
                         (* (get b-emojis % 0) t))
                     max-rank
                     starting-emojis)}))))

(comment
  (interpolate-keyframe-pair
   {:date   (t/date "2019-01-01")
    :emojis {"hot_sauce" 0}}
   {:date   (t/date "2020-01-01")
    :emojis {"hot_sauce" 100
             "damon" 6}}
   12
   3)

  )

(defn keyframes [interpolation-k max-rank timeseries-emojis]
  (let [frames (->> (partition 2 1 timeseries-emojis)
                    (mapcat (fn [[a b]] (interpolate-keyframe-pair a b interpolation-k max-rank)))
                    (into []))
        {:keys [date emojis]} (last timeseries-emojis)]
    (conj frames {:date (str (t/instant (t/at (t/date date) "00:00")))
                  :rank (rank #(get emojis %) max-rank emojis)})))

(defn keyframe-prev+next [keyframes]
  (let [nameframes (->> keyframes
                        (mapcat :rank)
                        (group-by :name))
        prev       (->> nameframes
                        (mapcat (fn [[_ emoji-keys]]
                                  (partition 2 1 emoji-keys)))
                        (mapcat identity)
                        (apply hash-map))]
    {:prev prev
     :next (clojure.set/map-invert prev)}))

(comment
  (let [ks (keyframes 2 2 [{:date (t/date "2019-01-01") :emojis {"hot_sauce" 0}}
                           {:date (t/date "2020-01-01") :emojis {"hot_sauce" 100 "damon" 6}}
                           {:date (t/date "2021-01-01") :emojis {"hot_sauce" 200 "damon" 306 "meow" 5}}])]
    (keyframe-prev+next ks))

  )
