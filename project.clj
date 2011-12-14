(defproject conrad "1.0.0-SNAPSHOT"
  :description "Back-End Services for the iPlant Administrative Console"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [compojure "0.6.5"]
                 [swank-clojure "1.3.1"]
                 [log4j/log4j "1.2.16"]
                 [korma "0.2.1"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler conrad.core/app}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
