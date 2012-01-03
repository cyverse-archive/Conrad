(ns conrad.app-crud
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

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
