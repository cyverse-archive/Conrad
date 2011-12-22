(ns conrad.category-listings
  (:use [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]))

(declare load-category load-subcategories)

(defn- count-templates [hid subcategories]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) FROM template_group_template
      WHERE template_group_id = ?" hid]
    (+ (:count (first rs)) (reduce + (map #(:template_count %) subcategories)))))

(defn- load-category [hid & fs]
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

(defn- get-public-root-category-hids []
  (jdbc/with-query-results rs
    ["SELECT * FROM workspace WHERE is_public IS TRUE"]
    (doall (map #(:root_analysis_group_id %) rs))))

(defn- get-app-ids-in-category [category-hid]
  (jdbc/with-query-results rs
    ["SELECT template_id FROM template_group_template
      WHERE template_group_id = ?" category-hid]
    (doall (map #(:template_id %) rs))))

(defn- load-apps-in-categories [category-hids]
  (map #(load-app %)
       (reduce concat (map #(get-app-ids-in-category %) category-hids))))

(defn- extract-category-hids [category]
  (cons (:hid category)
        (reduce concat (map #(extract-category-hids %) (:groups category)))))

(defn- marshal-apps-in-category [hid]
  (let [category (load-category hid)
        apps (load-apps-in-categories (extract-category-hids category))]
    (dissoc (assoc category :templates apps) :groups)))

(defn- get-category-hid [id]
  (jdbc/with-query-results rs
    ["SELECT hid FROM template_group WHERE id = ?" id]
    (if (empty? rs)
      (throw (IllegalArgumentException. (str "app group " id " not found")))
      (:hid (first rs)))))

(defn- marshal-category-without-apps [hid]
  (load-category hid #(dissoc % :hid :workspace_id)))

(defn list-public-categories-without-apps []
  (let [hids (get-public-root-category-hids)]
    {:groups (map #(marshal-category-without-apps %) hids)}))

(defn list-category-with-apps [category-id]
  (marshal-apps-in-category (get-category-hid category-id)))
