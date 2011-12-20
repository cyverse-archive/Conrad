(ns conrad.category-admin
  (:use [clojure.data.json :only (json-str)]
        [clojure.pprint :only (pprint)]
        [conrad.common]
        [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
	    [clojure-commons.json :as cc-json]))

(defn- pstr [obj]
  (with-out-str (pprint obj)))

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
      FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE tgt.template_group_id = ?
      AND a.deleted IS FALSE" hid]
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

(defn- find-parent-category-hids [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE subgroup_id = ?" hid]
    (doall (map #(:parent_group_id %) rs))))

(defn- get-subcategory-ids [parent-hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE parent_group_id = ?
      ORDER BY hid" parent-hid]
    (doall (map #(:subgroup_id %) rs))))

(defn- update-subcategory-hid [parent-id subcategory-id hid]
  (jdbc/update-values
   :template_group_group
   ["parent_group_id = ? AND subgroup_id = ?" parent-id subcategory-id]
   {:hid hid}))

(defn- ensure-contiguous-subcategory-hids-for-parent [parent-hid]
  (doseq [x (map vector (get-subcategory-ids parent-hid) (iterate inc 0))]
    (update-subcategory-hid parent-hid (first x) (last x))))

(defn- ensure-contiguous-subcategory-hids [parent-hids]
  (doseq [x parent-hids] (ensure-contiguous-subcategory-hids-for-parent x)))

(defn- delete-category-with-id [id]
  (let [category (load-category id)
        hid (:hid category)
        parent-hids (find-parent-category-hids hid)]
    (ensure-empty id hid)
    (jdbc/delete-rows :template_group_group ["subgroup_id = ?" hid])
    (jdbc/delete-rows :template_group_template ["template_group_id = ?" hid])
    (ensure-contiguous-subcategory-hids parent-hids)
    (jdbc/delete-rows :template_group ["hid = ?" hid])
    (success-response {:categoryId id})))

(defn rename-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-category-name (cc-json/body->json body)))))

(defn delete-category [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (delete-category-with-id id))))
