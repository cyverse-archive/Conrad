(ns conrad.core
  (:gen-class)
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params params]
        [clj-cas.cas-proxy-auth :only (validate-cas-group-membership)]
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

(defn- trap
"An error catching function."
[f]
  (try
    (f)
    (catch IllegalArgumentException e (failure-response e))
    (catch IllegalStateException e (failure-response e))
    (catch SQLException e (error-response (.getNextException e)))
    (catch Throwable t (error-response t))))

;; Secured routes.
(defroutes secured-routes
  (GET "/get-app-groups" []
       (trap #(get-public-categories)))

  (GET "/get-apps-in-group/:id" [id]
       (trap #(get-category-with-apps id)))

  (POST "/update-app" [:as {body :body}]
        (trap #(update-app body)))

  (POST "/rename-category" [:as {body :body}]
        (trap #(rename-category body)))

  (DELETE "/category/:id" [id]
          (trap #(delete-category id)))

  (PUT "/category" [:as {body :body}]
       (trap #(create-category body)))

  (DELETE "/app/:id" [id]
          (trap #(delete-app id)))

  (POST "/move-app" [:as {body :body}]
        (trap #(move-app body)))

  (GET "/undelete-app/:id" [id]
       (trap #(undelete-app id)))

  (POST "/move-category" [:as {body :body}]
        (trap #(move-category body)))

  (route/not-found (unrecognized-path-response)))

;; All routes.
(defroutes conrad-routes

  (GET "/" [] "Welcome to Conrad!\n")

  (context "/secured" []
           (validate-cas-group-membership
             secured-routes #(cas-server) #(server-name) #(group-attr-name)
             #(allowed-groups)))

  (route/not-found (unrecognized-path-response)))

(defn load-configuration
  "Loads the configuration properties from Zookeeper."
  []
  (cl/with-zk
    (zk-url)
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
