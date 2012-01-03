(ns conrad.app-crud
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log])
  (:import [java.sql Timestamp]))

(defn load-deployed-components-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM deployed_component_listing
      WHERE analysis_id = ?
      ORDER BY execution_order" app-hid]
    (doall rs)))

(defn list-app [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing WHERE hid = ?" hid]
    (first rs)))

(defn load-app-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM transformation_activity WHERE id = ?" id]
    (first rs)))

(defn- sql-timestamp [time]
  (if (or (nil? time) (= 0 time)) nil (Timestamp. time)))

(defn update-transformation-activity [app-update id]
  (jdbc/update-values :transformation_activity ["id = ?" id] app-update))

(defn update-integration-data [id integration-data-update]
  (jdbc/update-values :integration_data ["id = ?" id] integration-data-update))

(defn mark-app-deleted [id]
  (jdbc/update-values
   :transformation_activity ["id = ?" id]
   {:deleted true}))

(defn- remove-public-categorizations [app-hid]
  (jdbc/delete-rows
   :template_group_template
   ["template_id = ? AND template_group_id IN (
         SELECT tg.hid FROM template_group tg
         JOIN workspace w ON tg.workspace_id = w.id
         WHERE w.is_public IS TRUE
     )" app-hid]))

(defn- categorize-app [app-hid category-hid]
  (jdbc/insert-values
   :template_group_template
   [:template_id :template_group_id]
   [app-hid category-hid]))

(defn move-public-app [app-hid category-hid]
  (remove-public-categorizations app-hid)
  (categorize-app app-hid category-hid))
