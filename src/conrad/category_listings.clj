(ns conrad.category-listings
  (:use [conrad.app-listings :only (load-app-listing)]
        [conrad.category-crud])
  (:require [clojure.tools.logging :as log]))

(defn- load-apps-in-categories [category-hids]
  (map #(load-app-listing %)
       (reduce concat (map #(get-app-ids-in-category %) category-hids))))

(defn- extract-category-hids [category]
  (cons (:hid category)
        (reduce concat (map #(extract-category-hids %) (:groups category)))))

(defn- marshal-apps-in-category [hid]
  (let [category (load-category hid)
        apps (load-apps-in-categories (extract-category-hids category))]
    (dissoc (assoc category :templates apps) :groups)))

(defn- marshal-category-without-apps [hid]
  (load-category hid #(dissoc % :hid :workspace_id)))

(defn list-public-categories-without-apps []
  (let [hids (get-public-root-category-hids)]
    {:groups (map #(marshal-category-without-apps %) hids)}))

(defn list-category-with-apps [category-id]
  (marshal-apps-in-category (get-category-hid category-id)))
