(ns emojis.main
  (:require ["d3" :as d3]))

(defonce emojis (atom {}))

(def n 12)
(def duration 250)

(defn color [a]
  "red")

(def margin
  {:top 16
   :right 6
   :bottom 6
   :left 0})

(def bar-size 48)

(defn calc-height [margin bar-size n-bars]
  (+ (:top margin) (* bar-size n-bars) (:bottom margin)))

(def height (calc-height margin bar-size n))
(def width 1000)

(defn generate-x-scale [n-bars margin width]
  (d3/scaleLinear (clj->js [0 1]) (clj->js [(:left margin) (- width (:right margin))])))

(def x (generate-x-scale n margin width))

(defn generate-y-scale [n-bars margin bar-size]
  (.. (d3/scaleBand)
      (domain (clj->js (range (+ n-bars 1))))
      (rangeRound (clj->js [(:top margin)
                            (+ (:top margin)
                               (* bar-size
                                  (+ n-bars
                                     1
                                     0.1)))]))
      (padding 0.1)))

(def y (generate-y-scale n margin bar-size))

(defn safe-subvec [vec start-i end-i]
  (subvec vec start-i (min end-i (count vec))))

(defn bars [svg {:keys [x y]} {:keys [prev next]}]
  (let [bar (.. svg
                (append "g")
                (attr "fill-opacity" 0.6)
                (selectAll "rect"))]
    (fn [[date input-data] transition]
      (js/console.log input-data)
      (.. bar
          (data (safe-subvec input-data 0 n))
          (join
           (fn bars-enter [enter]
             (.. enter
                 (append "rect")
                 (attr "fill" color)
                 (attr "height" (.bandwidth y))
                 (attr "x" (x 0))
                 (attr "y" #(y (:rank (or (get prev %) %))))
                 (attr "width" #(- (x (:value (or (get prev %) %)))
                                   (x 0)))))
           (fn bars-update [update] update)
           (fn bars-exit [exit]
             (.. exit
                 (transition transition)
                 remove
                 (attr "y" #(y (:rank (or (get next %) %))))
                 (attr "width" #(- (x (:value (or (get next %) %)))
                                   (x 0))))))
          (call (fn [bar]
                  (.. bar
                      (transition transition)
                      (attr "y" #(y (:rank %)))
                      (attr "width" #(- (x (:value %))
                                        (x 0))))))))))

(defn animate [svg bar-chart-race-data]
  (let [keyframes   (->> (:keyframes bar-chart-race-data)
                         (map (juxt :date :rank)))
        update-bars (bars svg {:x x :y y} bar-chart-race-data)]

    (let [keyframe   (nth keyframes 5)
          transition (.. svg
                         transition
                         (duration duration)
                         (ease d3/easeLinear))]
      ;; Extract the top bar’s value and update the domain of x.
      ;; causes the chart to rerender
      (.domain x (clj->js [0 (get-in keyframe [1 0 "value"])]))

      (update-bars keyframe transition)

      ;;(js/Promise.await tr)
      )




    #_(doseq [keyframe keyframes]
        (let [transition (.. svg
                             transition
                             (duration duration)
                             (ease d3/easeLinear))]
          ;; Extract the top bar’s value and update the domain of x.
          ;; causes the chart to rerender
          (.domain x (clj->js [0 (get-in keyframe [1 0 "value"])]))

          (update-bars keyframe transition)

          ;;(js/Promise.await tr)
          )



        )
    ))

(defonce svg
  (let [svg (.append (d3/select "#emoji_app") "svg")
        svg (.attr svg "viewBox" (clj->js [0 0 100 100]))]
    svg))

(defonce bar-chart-race-year (get-in @emojis [:emojis-summary :bar-chart-race-year]))

(js/console.log (.data (animate svg bar-chart-race-year)))

(comment


  ;; how do you invoke the d3 component?
  (let [svg                 svg
        bar-chart-race-year (get-in @emojis [:emojis-summary :bar-chart-race-year])

        keyframes (->> (:keyframes bar-chart-race-year)
                       (map (juxt :date :rank)))]
    (first keyframes)
    )

  ;;for (const keyframe of keyframes) {
  ;;                                   const transition = svg.transition()
  ;;                                   .duration(duration)
  ;;                                   .ease(d3.easeLinear);

  ;;                                   // Extract the top bar’s value.
  ;;                                   x.domain([0, keyframe[1][0].value]);

  ;;                                   updateAxis(keyframe, transition);
  ;;                                   updateBars(keyframe, transition);
  ;;                                   updateLabels(keyframe, transition);
  ;;                                   updateTicker(keyframe, transition);

  ;;                                   invalidation.then(() => svg.interrupt());
  ;;                                   await transition.end();
  ;;                                   }







  )

(d3/pairs (clj->js [1, 2, 3, 5]))



(defn ^:export init [data]
  (reset! emojis (js->clj data :keywordize-keys true))
  (js/console.log "initialized"))
