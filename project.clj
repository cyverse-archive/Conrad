(defproject conrad "1.1.0-SNAPSHOT"
  :description "Back-End Services for the iPlant Administrative Console"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/java.jdbc "0.2.0"]
                 [org.iplantc/clojure-commons "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [swank-clojure "1.4.0-SNAPSHOT"]
                 [log4j/log4j "1.2.16"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [ring/ring-jetty-adapter "1.0.1"]]
  :dev-dependencies [[org.iplantc/lein-iplant-rpm "1.1.0-SNAPSHOT"]
                     [lein-ring "0.4.5"]
                     [swank-clojure "1.4.0-SNAPSHOT"]]
  :extra-classpath-dirs ["conf/test"]
  :aot [conrad.core]
  :main conrad.core
  :ring {:handler conrad.core/app :init conrad.core/load-configuration}
  :iplant-rpm {:summary "iPlant Conrad"
               :release 1
               :provides "conrad"
               :dependencies ["iplant-service-config >= 0.1.0-4"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
