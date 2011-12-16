(ns conrad.categories
  (:use [clojure.data.json :only (json-str)]
        [conrad.database])
  (:require [clojure.java.jdbc :as jdbc]))

(declare load-category load-subcategories)

(defn- count-templates [id subcategories]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) FROM template_group_template
      WHERE template_group_id = ?" id]
    (+ (:count (first rs)) (reduce + (map #(:template_count %) subcategories)))))

(defn- load-category [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_group_listing WHERE hid = ?" id]
    (let [subcategories (load-subcategories id)
          template-count (count-templates id subcategories)]
      (assoc (dissoc (first rs) :hid :workspace_id)
        :groups subcategories :template_count template-count))))

(defn- load-subcategories [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group WHERE parent_group_id = ?" id]
    (doall (map #(load-category (:subgroup_id %)) rs))))

(defn- marshal-category-without-apps [id]
  (load-category id))

(defn- get-public-root-category-ids []
  (jdbc/with-query-results rs
    ["SELECT * FROM workspace WHERE is_public IS TRUE"]
    (doall (map #(:root_analysis_group_id %) rs))))

(defn- marshal-public-categories-without-apps []
  (let [ids (get-public-root-category-ids)]
    {:groups (map #(marshal-category-without-apps %) ids)}))

(defn get-public-categories []
  (jdbc/with-connection (db-connection)
    (json-str (marshal-public-categories-without-apps))))
