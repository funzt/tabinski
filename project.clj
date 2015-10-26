(defproject org.funzt/tabinski "0.1.0-SNAPSHOT"
  :description "Tab-friendliness in React based applications"
  :url "http://github.com/funzt/tabinski"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [minreact "0.1.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "1.7.145"]
                   [com.cemerick/piggieback "0.2.1"]
                   [sablono "0.3.6"]]
    :plugins [[lein-cljsbuild "1.1.0"]
              [lein-figwheel "0.4.1"]]
    :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]
    
    :figwheel {:nrepl-port 7888
               :nrepl-middleware ["cider.nrepl/cider-middleware"
                                  "cemerick.piggieback/wrap-cljs-repl"]}
    
    :cljsbuild
    {:builds
     [{:id "dev"
       :source-paths ["src" "dev"]
       :figwheel true
       :compiler {:main dev.core
                  :asset-path "cljs/out"
                  :output-to  "resources/public/cljs/main.js"
                  :output-dir "resources/public/cljs/out"}}]}}})
