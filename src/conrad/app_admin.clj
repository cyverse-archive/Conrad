(ns conrad.app-admin
  (:use [clojure.data.json :only (json-str read-json)]
        [conrad.database])
  (:require [clojure.java.jdbc :as jdbc]
	    [clojure-commons.json :as cc-json])
  (:import [java.sql.Timestamp]))

(defn- success-response [& data]
  {:success true
   :total (count data)
   :data data})

(defn- load-transformation-activity [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM transformation_activity WHERE id = ?" id]
    (first rs)))

(defn- sql-timestamp [time]
  (if (or (nil? time) (= 0 time)) nil (java.sql.Timestamp. time)))

(defn- update-transformation-activity [app-info id]
  (jdbc/update-values
   :transformation_activity ["id = ?" id]
   {:name (:name app-info)
    :description (:description app-info)
    :integration_date (sql-timestamp (:integration_date app-info))
    :wikiurl (:wiki_url app-info)}))

(defn- update-integration-data [id app-info]
  (jdbc/update-values
   :integration_data ["id = ?" id]
   {:integrator_name (:integrator_name app-info)
    :integrator_email (:integrator_email app-info)}))

(defn- update-app-info [app-info]
  (let [id (:id app-info)
        transformation-activity (load-transformation-activity id)
        integration-data-id (:integration_data_id transformation-activity)]
    (if (nil? transformation-activity)
      (throw (IllegalArgumentException. (str "app, " id ", not found"))))
    (update-transformation-activity app-info id)
    (update-integration-data integration-data-id app-info)
    (success-response)))

(defn update-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (json-str (update-app-info (cc-json/body->json body))))))
