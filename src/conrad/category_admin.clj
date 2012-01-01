(ns conrad.category-admin
  (:use [clojure.contrib.datalog.util :only (keys-to-vals)]
        [clojure.data.json :only (json-str)]
        [clojure.pprint :only (pprint)]
        [conrad.common]
        [conrad.database]
        [conrad.app-listings]
        [conrad.category-listings])
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

(defn- extract-category-description [category-info]
  (get category-info :description ""))

(defn- extract-parent-category-id [category-info]
  (let [parent-category-id (:parentCategoryId category-info)]
    (if (empty? parent-category-id)
      (throw (IllegalArgumentException. "parent category ID not specified")))
    parent-category-id))

(defn- update-category-name [category-info]
  (let [id (extract-category-id category-info)
        name (extract-category-name category-info)
        category (load-category-by-id id)
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
  (let [category (load-category-by-id id)
        hid (:hid category)
        parent-hids (find-parent-category-hids hid)]
    (ensure-empty id hid)
    (jdbc/delete-rows :template_group_group ["subgroup_id = ?" hid])
    (jdbc/delete-rows :template_group_template ["template_group_id = ?" hid])
    (ensure-contiguous-subcategory-hids parent-hids)
    (jdbc/delete-rows :template_group ["hid = ?" hid])
    (success-response {:categoryId id})))

(defn- ensure-category-doesnt-exist [parent-id parent-hid name]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count FROM template_group_group tgg
      JOIN template_group tg ON tgg.subgroup_id = tg.hid
      WHERE tgg.parent_group_id = ?
      AND name = ?" parent-hid name]
    (if (> (:count (first rs)) 0)
      (throw (IllegalStateException.
              (str "category, " parent-id ", already contains a subcategory "
                   "named, \"" name "\""))))))

(defn- ensure-category-doesnt-contain-apps [parent-id parent-hid]
  (if (not= 0 (count-apps-in-category parent-hid))
    (throw (IllegalStateException.
            (str "category, " parent-id ", contains apps")))))

(defn- insert-category [args]
  (let [vals (conj (vec (map #(get args %) [:id :name :description])) 0)]
    (log/warn vals)
    (jdbc/insert-values
     :template_group
     [:id :name :description :workspace_id] vals)))

(defn- get-next-grouping-hid [parent-category-hid]
  (jdbc/with-query-results rs
    ["SELECT COALESCE(MAX(hid) + 1, 0) AS next_hid
      FROM template_group_group
      WHERE parent_group_id = ?" parent-category-hid]
    (:next_hid (first rs))))

(defn- group-category [parent-category-hid child-category-hid]
  (let [grouping-hid (get-next-grouping-hid parent-category-hid)]
    (jdbc/insert-values
     :template_group_group
     [:parent_group_id :subgroup_id :hid]
     [parent-category-hid child-category-hid grouping-hid])))

(defn- insert-and-group-category [args]
  (insert-category args)
  (let [category (load-category-by-id (:id args))]
    (group-category (:parent-category-hid args) (:hid category))
    (success-response {:category (list-category-with-apps (:id category))})))

(defn- create-new-category [category-info]
  (let [parent-id (extract-parent-category-id category-info)
        name (extract-category-name category-info)
        description (extract-category-description category-info)
        parent-category (load-category-by-id parent-id)]
    (ensure-category-doesnt-exist parent-id (:hid parent-category) name)
    (ensure-category-doesnt-contain-apps parent-id (:hid parent-category))
    (insert-and-group-category {:name name
                                :description description
                                :parent-category-hid (:hid parent-category)
                                :id (uuid)})))

(defn rename-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-category-name (cc-json/body->json body)))))

(defn delete-category [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (delete-category-with-id id))))

(defn create-category [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (create-new-category (cc-json/body->json body)))))
