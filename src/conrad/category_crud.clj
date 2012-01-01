(ns conrad.category-crud
  (:use [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]))

(declare load-category load-subcategories)

(defn- count-templates [hid subcategories]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) FROM template_group_template
      WHERE template_group_id = ?" hid]
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

