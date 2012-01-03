(ns conrad.category-admin
  (:use [clojure.contrib.datalog.util :only (keys-to-vals)]
        [clojure.data.json :only (json-str)]
        [conrad.common :only (success-response uuid)]
        [conrad.category-listings :only (list-category-with-apps)]
        [conrad.database :only (db-connection)]
        [conrad.category-crud])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
	    [clojure-commons.json :as cc-json]))

(defn- extract-category-id [category-info]
  (let [id (:categoryId category-info)]
    (if (empty? id)
      (throw (IllegalArgumentException. "category ID not specified")))
    id))

(defn- extract-category-name [category-info]
  (let [name (:name category-info)]
    (if (empty? name)
      (throw (IllegalArgumentException. "category name not specified")))
    name))

(defn- extract-category-description [category-info]
  (get category-info :description ""))

(defn- extract-parent-category-id [category-info]
  (let [parent-category-id (:parentCategoryId category-info)]
    (if (empty? parent-category-id)
      (throw (IllegalArgumentException. "parent category ID not specified")))
    parent-category-id))

(defn- rename-category* [category-info]
  (let [id (extract-category-id category-info)
        name (extract-category-name category-info)
        category (load-category-by-id id)
        hid (:hid category)]
    (update-category-name hid name)
    (success-response {:name name})))

(defn- create-category* [category-info]
  (let [parent-id (extract-parent-category-id category-info)
        name (extract-category-name category-info)
        description (extract-category-description category-info)
        parent-category (load-category-by-id parent-id)
        id (uuid)]
    (insert-and-group-category {:name name
                                :description description
                                :parent-category-id parent-id
                                :parent-category-hid (:hid parent-category)
                                :id id})
    (success-response {:category (list-category-with-apps id)})))

(defn rename-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (rename-category* (cc-json/body->json body)))))

(defn delete-category [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (delete-category-with-id id))
    (success-response {:categoryId id})))

(defn create-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (create-category* (cc-json/body->json body)))))
