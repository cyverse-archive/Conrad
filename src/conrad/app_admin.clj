(ns conrad.app-admin
  (:use [conrad.common]
        [conrad.database]
        [conrad.app-listings]
        [conrad.app-crud]
        [conrad.category-crud]
        [conrad.category-listings])
  (:require [clojure.java.jdbc :as jdbc]
	    [clojure-commons.json :as cc-json]))

(defn- load-transformation-activity [id]
  (let [transformation-activity (load-app-by-id id)]
    (if (nil? transformation-activity)
      (throw (IllegalArgumentException. (str "app, " id ", not found"))))
    transformation-activity))

(defn- app-info->app-update [app-info]
  {:name (:name app-info)
   :description (:description app-info)
   :integration_date (:integration_date app-info)
   :wikiurl (:wiki_url app-info)})

(defn- app-info->integration-data-update [app-info]
  {:integrator_name (:integrator_name app-info)
   :integrator_email (:integrator_email app-info)})

(defn- update-app-info [app-info]
  (let [id (:id app-info)
        transformation-activity (load-transformation-activity id)
        hid (:hid transformation-activity)
        integration-data-id (:integration_data_id transformation-activity)]
    (update-transformation-activity (app-info->app-update app-info) id)
    (update-integration-data integration-data-id
                             (app-info->integration-data-update app-info))
    (success-response {:application (load-app-listing hid)})))

(defn- move-app-to-new-category [categorization-info]
  (let [app-id (extract-required-field categorization-info :id)
        category-id (extract-required-field categorization-info :categoryId)
        transformation-activity (load-transformation-activity app-id)
        category (load-category-by-id category-id)
        app-hid (:hid transformation-activity)
        category-hid (:hid category)]
    (ensure-category-doesnt-contain-subcategories category-id category-hid)
    (move-public-app app-hid category-hid)
    (success-response {:category (list-category-with-apps category-id)})))

(defn update-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-app-info (cc-json/body->json body)))))

(defn delete-app [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction
     (load-transformation-activity id)
     (mark-app-deleted id)
     (success-response {:id id}))))

(defn move-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (move-app-to-new-category (cc-json/body->json body)))))
