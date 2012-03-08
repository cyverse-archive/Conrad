(ns conrad.core
  (:gen-class)
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params params]
        [clojure-commons.cas-proxy-auth]
        [clojure-commons.filtered-routes]
        [clojure-commons.query-params :only (wrap-query-params)]
        [conrad.app-admin]
        [conrad.category-admin]
        [conrad.common]
        [conrad.config]
        [conrad.listings]
        [conrad.database]
        [clojure.data.json :only (json-str)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure-commons.clavin-client :as cl]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty])
  (:import [java.sql SQLException]))

(defn- trap [f]
  (try
    (f)
    (catch IllegalArgumentException e (failure-response e))
    (catch IllegalStateException e (failure-response e))
    (catch SQLException e (do (log-next-exception e) (error-response e)))
    (catch Throwable t (error-response t))))

(defroutes conrad-routes

  (GET "/" [] "Welcome to Conrad!\n")

  (FILTERED-GET
    "/get-app-groups" []
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(get-public-categories)))

  (FILTERED-GET
    "/get-apps-in-group/:id" [id]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(get-category-with-apps id)))

  (FILTERED-POST
    "/update-app" [:as {body :body}]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(update-app body)))

  (FILTERED-POST
    "/rename-category" [:as {body :body}]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(rename-category body)))

  (FILTERED-DELETE
    "/category/:id" [id]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(delete-category id)))

  (FILTERED-PUT
    "/category" [:as {body :body}]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(create-category body)))

  (FILTERED-DELETE
    "/app/:id" [id]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(delete-app id)))

  (FILTERED-POST
    "/move-app" [:as {body :body}]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(move-app body)))

  (FILTERED-GET
    "/undelete-app/:id" [id]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(undelete-app id)))

  (FILTERED-POST
    "/move-category" [:as {body :body}]
    [validate-cas-proxy-ticket #(cas-server) #(server-name)]
    (trap #(move-category body)))

  (route/not-found (unrecognized-path-response)))

(defn load-configuration
  "Loads the configuration properties from Zookeeper."
  []
  (cl/with-zk
    zk-url
    (when (not (cl/can-run?))
      (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
      (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY.")
      (System/exit 1))
    (reset! props (cl/properties "conrad")))
  (log/warn @props)
  (when (not (configuration-valid))
    (log/warn "THE CONFIGURATION IS INVALID - EXITING NOW")
    (System/exit 1)))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params
      wrap-query-params))

(def app
  (site-handler conrad-routes))

(defn -main
  [& args]
  (load-configuration)
  (log/warn "Listening on" (listen-port))
  (jetty/run-jetty app {:port (listen-port)}))
