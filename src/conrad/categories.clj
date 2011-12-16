(ns conrad.categories
  (:use [clojure.pprint]
        [clojure.data.json :only (json-str)]
        [conrad.database])
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

(defn- marshal-category-without-apps [hid]
  (load-category hid #(dissoc % :hid :workspace_id)))

(defn- get-public-root-category-hids []
  (jdbc/with-query-results rs
    ["SELECT * FROM workspace WHERE is_public IS TRUE"]
    (doall (map #(:root_analysis_group_id %) rs))))

(defn- marshal-public-categories-without-apps []
  (let [hids (get-public-root-category-hids)]
    {:groups (map #(marshal-category-without-apps %) hids)}))

(defn get-public-categories []
  (jdbc/with-connection (db-connection)
    (json-str (marshal-public-categories-without-apps))))

(defn- time-from-sql-timestamp [ts]
  (if (nil? ts) nil (.getTime ts)))

(defn- assoc-if-not-nil [map key value & kvs]
  (let [ret (if (nil? value) (dissoc map key) (assoc map key value))]
    (if kvs
      (recur ret (first kvs) (second kvs) (nnext kvs))
      ret)))
(defn- step-count-msg [id, adj]
  (str "analysis, " id ", has too " adj " steps for a pipeline"))

(defn- app-pipeline-eligibility [app]
  (let [step-count (:step_count app) id (:id app)]
    (cond
     (< step-count 1) {:is_valid false :reason (step-count-msg id "few")}
     (> step-count 1) {:is_valid false :reason (step-count-msg id "many")}
     :else {:is_valid true :reason ""})))

(defn- normalize-deployed-component-listing [dc]
  {:id (:deployed_component_id dc)
   :name (:name dc)
   :description (:description dc)
   :location (:location dc)
   :type (:type dc)
   :version (:version dc)
   :attribution (:attribution dc)})

(defn- app-deployed-component-listing [app]
  (jdbc/with-query-results rs
    ["SELECT * FROM deployed_component_listing
      WHERE analysis_id = ?
      ORDER BY execution_order" (:hid app)]
    (doall (map #(normalize-deployed-component-listing %) rs))))

(defn- normalize-app-listing [app]
  {:id (:id app)
   :name (:name app)
   :description (:description app)
   :integrator_email (:integrator_email app)
   :integrator_name (:integrator_name app)
   :rating {:average (:average_rating app)}
   :is_public (:is_public app)
   :is_favorite false
   :wiki_url (:wikiurl app)
   :deployed_components (app-deployed-component-listing app)
   :pipeline_eligibility (app-pipeline-eligibility app)})

(defn- load-app [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing WHERE hid = ?" hid]
    (normalize-app-listing (first rs))))

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

(defn get-category-with-apps [category-id]
  (jdbc/with-connection (db-connection)
    (json-str (marshal-apps-in-category (get-category-hid category-id)))))
