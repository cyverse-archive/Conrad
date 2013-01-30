(defproject conrad "1.3.0-SNAPSHOT"
  :description "Back-End Services for the iPlant Administrative Console"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.iplantc/clj-cas "1.0.0-SNAPSHOT"]
                 [org.iplantc/kameleon "0.1.0-SNAPSHOT"]
                 [org.iplantc/clojure-commons "1.4.0-SNAPSHOT"]
                 [cheshire "5.0.1"]
                 [compojure "1.0.1"]
                 [swank-clojure "1.4.0-SNAPSHOT"]
                 [log4j/log4j "1.2.16"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [korma/korma "0.3.0-RC2"]
                 [ring/ring-jetty-adapter "1.0.1"]
                 [postgresql/postgresql "9.0-801.jdbc4"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.0-SNAPSHOT"]
            [lein-ring "0.7.1"]
            [swank-clojure "1.4.2"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [conrad.core]
  :main conrad.core
  :ring {:handler conrad.core/app
         :init conrad.core/load-configuration-from-file
         :port 31334}
  :iplant-rpm {:summary "iPlant Conrad"
               :provides "conrad"
               :dependencies ["iplant-service-config >= 0.1.0-5"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
