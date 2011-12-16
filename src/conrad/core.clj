(ns conrad.core
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params]
        [conrad.app-admin]
        [conrad.listings]
        [conrad.database])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes conrad-routes
  (GET "/" [] "Welcome to Conrad!\n")
  (GET "/get-app-groups" [] (get-public-categories))
  (GET "/get-apps-in-group/:id" [id] (get-category-with-apps id))
  (POST "/update-app" [:as {body :body}] (update-app body))
  (route/not-found "Unrecognized service path.\n"))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params))

(def app
  (site-handler conrad-routes))
