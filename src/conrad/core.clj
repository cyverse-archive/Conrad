(ns conrad.core
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params]
        [conrad.app-admin]
        [conrad.category-admin]
        [conrad.common]
        [conrad.listings]
        [conrad.database]
        [clojure.data.json :only (json-str)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
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
  (GET "/get-app-groups" [] (trap #(get-public-categories)))
  (GET "/get-apps-in-group/:id" [id] (trap #(get-category-with-apps id)))
  (POST "/update-app" [:as {body :body}] (trap #(update-app body)))
  (POST "/rename-category" [:as {body :body}] (trap #(rename-category body)))
  (DELETE "/category/:id" [id] (trap #(delete-category id)))
  (PUT "/category" [:as {body :body}] (trap #(create-category body)))
  (DELETE "/app/:id" [id] (trap #(delete-app id)))
  (POST "/move-app" [:as {body :body}] (trap #(move-app body)))
  (POST "/move-category" [:as {body :body}] (trap #(move-category body)))
  (route/not-found (unrecognized-path-response)))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params))

(def app
  (site-handler conrad-routes))
