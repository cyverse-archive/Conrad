(ns conrad.core
  (:use [compojure.core]
        [clojure-commons.props :as cc-props]
        [ring.middleware keyword-params nested-params]
        [clojure.data.json :only (json-str)]
        [korma.db]
        [conrad.config])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defn get-analysis-groups []
  (json-str {:groups []}))

(defroutes conrad-routes
  (GET "/" [] "Welcome to Conrad!\n")
  (GET "/analysis-groups" [] (get-analysis-groups))
  (route/not-found "Unrecognized service path.\n"))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params))

(def app
  (site-handler conrad-routes))
