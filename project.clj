(defproject conrad "1.0.0-SNAPSHOT"
  :description "Back-End Services for the iPlant Administrative Console"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/java.jdbc "0.1.0"]
                 [org.iplantc/clojure-commons "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [swank-clojure "1.4.0-SNAPSHOT"]
                 [log4j/log4j "1.2.16"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [ring/ring-jetty-adapter "1.0.1"]
                 [org.jasig.cas.client/cas-client-core "3.2.0"
                  :exclusions [javax.servlet/servlet-api]]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [swank-clojure "1.4.0-SNAPSHOT"]]
  :extra-classpath-dirs ["conf"]
  :aot [conrad.core]
  :main conrad.core
  :ring {:handler conrad.core/app :init conrad.core/load-configuration}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
