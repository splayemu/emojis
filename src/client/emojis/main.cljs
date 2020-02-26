(ns emojis.main)

(defn ^:export init [data]
  (js/console.log "meow")
  (js/console.log (js->clj data)))
