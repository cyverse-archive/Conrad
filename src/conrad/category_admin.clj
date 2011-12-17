(ns conrad.category-admin
  (:use [clojure.contrib.except :only (throw-if)]
        [clojure.data.json :only (json-str)]
        [conrad.common]
        [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]
	    [clojure-commons.json :as cc-json]))

(defn- extract-category-id [category-info]
  (let [id (:categoryId category-info)]
    (throw-if (empty? id)
              (IllegalArgumentException. "category ID not specified"))
    id))

(defn- extract-category-name [category-info]
  (let [name (:name category-info)]
    (throw-if (empty? name)
              (IllegalArgumentException. "category name not specified"))
    name))

(defn- update-category-name [category-info]
  (let [id (extract-category-id category-info)
        name (extract-category-name category-info)]
    (jdbc/update-values
     :template_group ["id = ?" id]
     {:name name})
    (success-response {:name name})))

(defn rename-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-category-name (cc-json/body->json body)))))
