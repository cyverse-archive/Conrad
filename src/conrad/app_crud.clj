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

(defn load-public-categories-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT tg.* FROM template_group_template tgt
      JOIN template_group tg ON tgt.template_group_id = tg.hid
      JOIN workspace w ON tg.workspace_id = w.id
      WHERE w.is_public
      AND tgt.template_id = ?" app-hid]
    (doall (map #(dissoc % :hid) rs))))

(defn list-app [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing WHERE hid = ?" hid]
    (first rs)))

(defn list-deleted-and-orphaned-apps []
  (jdbc/with-query-results rs
    ["SELECT a.hid, a.id, a.name, a.description, i.integrator_name,
          i.integrator_email, a.integration_date, a.wikiurl,
          CAST(COALESCE(AVG(r.rating), 0.0) AS DOUBLE PRECISION)
              AS average_rating,
          (EXISTS (
              SELECT * FROM template_group_template tgt
              JOIN template_group tg ON tgt.template_group_id = tg.hid
              JOIN workspace w ON tg.workspace_id = w.id
              WHERE a.hid = tgt.template_id
              AND w.is_public)) AS is_public,
          (SELECT COUNT(*)
              FROM transformation_task_steps tts
              WHERE tts.transformation_task_id = a.hid) AS step_count
      FROM transformation_activity a
          LEFT JOIN integration_data i ON a.integration_data_id = i.id
          LEFT JOIN ratings r ON a.hid = r.transformation_activity_id
      WHERE (a.deleted AND EXISTS (
              SELECT * FROM template_group_template tgt
              JOIN template_group tg ON tgt.template_group_id = tg.hid
              JOIN workspace w ON tg.workspace_id = w.id
              WHERE tgt.template_id = a.hid AND w.is_public))
          OR NOT EXISTS (
              SELECT * FROM template_group_template tgt
              WHERE a.hid = tgt.template_id)
      GROUP BY a.hid, a.id, a.name, a.description, i.integrator_name,
               i.integrator_email, a.integration_date, a.wikiurl"]
    (doall rs)))

(defn load-app-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM transformation_activity WHERE id = ?" id]
    (first rs)))

(defn- sql-timestamp [time]
  (if (or (nil? time) (= 0 time)) nil (Timestamp. time)))

(defn- convert-integration-date [app-update]
  (assoc app-update
    :integration_date (sql-timestamp (:integration_date app-update))))

(defn update-transformation-activity [app-update id]
  (jdbc/update-values :transformation_activity ["id = ?" id]
                      (convert-integration-date app-update)))

(defn update-integration-data [id integration-data-update]
  (jdbc/update-values :integration_data ["id = ?" id] integration-data-update))

(defn set-app-deleted-flag [id flag]
  (log/debug (str "setting the deleted flag for app, " id " to " flag))
  (jdbc/update-values
   :transformation_activity ["id = ?" id]
   {:deleted flag}))

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

(defn load-suggested-categories-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT tg.* FROM suggested_groups sg
      JOIN template_group tg ON sg.template_group_id = tg.hid
      WHERE sg.transformation_activity_id = ?" app-hid]
    (doall (map #(dissoc % :hid) rs))))
