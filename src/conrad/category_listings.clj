(ns conrad.category-listings
  (:use [conrad.database]
        [conrad.app-listings]
        [conrad.category-crud])
  (:require [clojure.java.jdbc :as jdbc]))

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
