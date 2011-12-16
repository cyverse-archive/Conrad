(ns conrad.core
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params]
        [conrad.categories]
        [conrad.database])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes conrad-routes
  (GET "/" [] "Welcome to Conrad!\n")
  (GET "/get-app-groups" [] (get-public-categories))
  (route/not-found "Unrecognized service path.\n"))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params))

(def app
  (site-handler conrad-routes))
