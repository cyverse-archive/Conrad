(ns conrad.category-crud
  (:use [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]))

(declare load-category load-subcategories)

(defn load-category-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group WHERE id = ?" id]
    (let [category (first rs)]
      (if (nil? category)
        (throw (IllegalArgumentException.
                (str "category, " id ", does not exist"))))
      category)))

(defn- count-templates [hid subcategories]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE NOT a.deleted
      AND template_group_id = ?" hid]
    (+ (:count (first rs)) (reduce + (map #(:template_count %) subcategories)))))

(defn load-category [hid & fs]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_group_listing WHERE hid = ?" hid]
    (let [f (first fs)
          subcategories (load-subcategories hid f)
          category (assoc (first rs)
                     :template_count (count-templates hid subcategories)
                     :groups subcategories)]
      (if (nil? f) category (f category)))))

(defn- load-subcategories [hid f]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group WHERE parent_group_id = ?" hid]
    (doall (map #(load-category (:subgroup_id %) f) rs))))

(defn get-public-root-category-hids []
  (jdbc/with-query-results rs
    ["SELECT * FROM workspace WHERE is_public IS TRUE"]
    (doall (map #(:root_analysis_group_id %) rs))))

(defn update-category-name [hid name]
    (jdbc/update-values
     :template_group ["hid = ?" hid]
     {:name name}))

(defn count-apps-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE tgt.template_group_id = ?
      AND a.deleted IS FALSE" hid]
    (:count (first rs))))

(defn count-subcategories-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_group
      WHERE parent_group_id = ?" hid]
    (:count (first rs))))

(defn find-parent-category-hids [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE subgroup_id = ?" hid]
    (doall (map #(:parent_group_id %) rs))))

(defn- ensure-empty [id hid]
  (if (not= 0 (count-apps-in-category hid))
    (throw (IllegalStateException. (str "category, " id ", contains apps"))))
  (if (not= 0 (count-subcategories-in-category hid))
    (throw (IllegalStateException.
            (str "category, " id ", contains subcategories")))))

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

(defn delete-category-with-id [id]
  (let [category (load-category-by-id id)
        hid (:hid category)
        parent-hids (find-parent-category-hids hid)]
    (ensure-empty id hid)
    (jdbc/delete-rows :template_group_group ["subgroup_id = ?" hid])
    (jdbc/delete-rows :template_group_template ["template_group_id = ?" hid])
    (ensure-contiguous-subcategory-hids parent-hids)
    (jdbc/delete-rows :template_group ["hid = ?" hid])))

(defn- get-next-grouping-hid [parent-category-hid]
  (jdbc/with-query-results rs
    ["SELECT COALESCE(MAX(hid) + 1, 0) AS next_hid
      FROM template_group_group
      WHERE parent_group_id = ?" parent-category-hid]
    (:next_hid (first rs))))

(defn- ensure-category-doesnt-contain-apps [parent-id parent-hid]
  (if (not= 0 (count-apps-in-category parent-hid))
    (throw (IllegalStateException.
            (str "category, " parent-id ", contains apps")))))

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

(defn- insert-category [args]
  (let [vals (conj (vec (map #(get args %) [:id :name :description])) 0)]
    (jdbc/insert-values
     :template_group
     [:id :name :description :workspace_id] vals)))

(defn- group-category [parent-category-hid child-category-hid]
  (let [grouping-hid (get-next-grouping-hid parent-category-hid)]
    (jdbc/insert-values
     :template_group_group
     [:parent_group_id :subgroup_id :hid]
     [parent-category-hid child-category-hid grouping-hid])))

(defn validate-category-insertion [args]
  (let [parent-id (:parent-category-id args)
        parent-hid (:parent-category-hid args)
        name (:name args)]
    (ensure-category-doesnt-exist parent-id parent-hid name)
    (ensure-category-doesnt-contain-apps parent-id parent-hid)))

(defn insert-and-group-category [args]
  (validate-category-insertion args)
  (insert-category args)
  (let [category (load-category-by-id (:id args))]
    (group-category (:parent-category-hid args) (:hid category))))
