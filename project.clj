(defproject emojis "0.1.0-SNAPSHOT"
  :description ""
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  ;; for shadow-cljs
  :source-paths ["src/server" "src/shared" "src/client"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [thheller/shadow-cljs "2.8.88"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.10.0"]
                 [aero "1.1.5"]
                 [http-kit "2.3.0"]
                 [stasis "2.5.0"]
                 [hiccup "1.0.5"]
                 [ring "1.2.1"]]
  :repl-options {:init-ns emojis.core})
