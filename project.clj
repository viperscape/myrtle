(defproject myrtle "0.1.2"
  :description "clojure IDE in the browser"
  :url "https://github.com/viperscape/myrtle"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [http-kit "2.1.12"]
                 [alembic "0.2.0"]
                 [ring "1.2.1"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [clojure-complete "0.2.3"]]
  ;:plugins [[lein-ring "0.8.8"]]
  :ring {:handler myrtle.handler/app}
  :main myrtle.handler)
  ;:profiles
  ;{:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
   ;                     [ring-mock "0.1.5"]]}}
  ;                      )
