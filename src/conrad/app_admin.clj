(ns conrad.app-admin
  (:use [conrad.common]
        [conrad.database]
        [conrad.app-listings]
        [conrad.category-crud]
        [conrad.category-listings])
  (:require [clojure.java.jdbc :as jdbc]
	    [clojure-commons.json :as cc-json])
  (:import [java.sql Timestamp Types]))

(defn- load-transformation-activity [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM transformation_activity WHERE id = ?" id]
    (if (= 0 (count rs))
      (throw (IllegalArgumentException. (str "app, " id ", not found"))))
    (first rs)))

(defn- sql-timestamp [time]
  (if (or (nil? time) (= 0 time)) nil (Timestamp. time)))

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
        hid (:hid transformation-activity)
        integration-data-id (:integration_data_id transformation-activity)]
    (update-transformation-activity app-info id)
    (update-integration-data integration-data-id app-info)
    (success-response {:application (load-app hid)})))

(defn- mark-app-deleted [id]
  (let [transformation-activity (load-transformation-activity id)
        hid (:hid transformation-activity)]
    (jdbc/update-values
     :transformation_activity ["id = ?" id]
     {:deleted true})
    (success-response {:id id})))

(defn- remove-public-categorizations [app-hid]
  (jdbc/delete-rows
   :template_group_template
   ["template_id = ? AND template_group_id IN (
         SELECT tg.hid FROM template_group tg
         JOIN workspace w ON tg.workspace_id = w.id
         WHERE w.is_public IS TRUE
     )" app-hid]))

(defn- categorize-app [app-hid category-hid]
  (jdbc/insert-values
   :template_group_template
   [:template_id :template_group_id]
   [app-hid category-hid]))

(defn- move-app-to-new-category [categorization-info]
  (let [app-id (extract-required-field categorization-info :id)
        category-id (extract-required-field categorization-info :categoryId)
        transformation-activity (load-transformation-activity app-id)
        category (load-category-by-id category-id)
        app-hid (:hid transformation-activity)
        category-hid (:hid category)]
    (remove-public-categorizations app-hid)
    (categorize-app app-hid category-hid)
    (success-response {:category (list-category-with-apps category-id)})))

(defn update-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-app-info (cc-json/body->json body)))))

(defn delete-app [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (mark-app-deleted id))))

(defn move-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (move-app-to-new-category (cc-json/body->json body)))))
