(ns conrad.category-admin
  (:use [clojure.data.json :only (json-str)]
        [conrad.common]
        [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]
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

(defn- load-category [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group WHERE id = ?" id]
    (let [category (first rs)]
      (if (nil? category)
        (throw (IllegalArgumentException.
                (str "category, " id ", does not exist"))))
      category)))

(defn- update-category-name [category-info]
  (let [id (extract-category-id category-info)
        name (extract-category-name category-info)
        category (load-category id)
        hid (:hid category)]
    (jdbc/update-values
     :template_group ["hid = ?" hid]
     {:name name})
    (success-response {:name name})))

(defn- count-apps-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_template
      WHERE template_group_id = ?" hid]
    (:count (first rs))))

(defn- count-subcategories-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_group
      WHERE parent_group_id = ?" hid]
    (:count (first rs))))

(defn- ensure-empty [id hid]
  (if (not= 0 (count-apps-in-category hid))
    (throw (IllegalStateException. (str "category, " id ", contains apps"))))
  (if (not= 0 (count-subcategories-in-category hid))
    (throw (IllegalStateException.
            (str "category, " id ", contains subcategories")))))

(defn- ensure-contiguous-subgroup-hids []
  ;; TODO: implement me
  )

(defn- delete-category-with-id [id]
  (let [category (load-category id)
        hid (:hid category)]
    (ensure-empty id hid)
    (jdbc/delete-rows :template_group_group ["subgroup_id = ?" hid])
    (ensure-contiguous-subgroup-hids)
    (jdbc/delete-rows :template_group ["hid = ?" hid])
    (success-response {:categoryId id})))

(defn rename-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-category-name (cc-json/body->json body)))))

(defn delete-category [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (delete-category-with-id id))))
