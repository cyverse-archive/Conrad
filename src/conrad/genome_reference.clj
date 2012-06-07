(ns conrad.genome-reference
  (:use [conrad.kormadb]
        [clojure.data.json :only (json-str)]
        [korma.core]
        [korma.db]
        [kameleon.entities])
  (:require [clojure.tools.logging :as log]))

(defn get-genome-references
"This function returns a JSON representation of the map of all the genome_reference table data in the DB (for testing purposes)" []
(json-str (str (select genome_reference))))

(defn get-genome-references-by-username
"This function returns a JSON representation of the map of all the genome_reference table data in the DB that was created by the passed username." [username]
(log/warn "Username Passed ="username)
(json-str (str (select genome_reference
                    (join users (= :users.id :genome_reference.created_by))
                    (where {:users.username username})))))

;(defn delete-genome-references-by-UUID
;"This function sets the deleted column of the genome_reference that matches the passed UUID's genome_reference table to true." [uuid]
;(log/warn "UUID Passed ="uuid)
;(str (dry-run (update genome_reference
;                    (set-fields {:deleted true})
;                    (where {:uuid uuid})))))

(defn delete-genome-references-by-UUID
"This function updates the deleted column of the genome_reference that matches the passed UUID's genome_reference table to true." [uuid]
(log/warn "UUID Passed ="uuid)
(exec-raw ["UPDATE \"genome_reference\" SET \"deleted\" = TRUE WHERE (\"genome_reference\".\"uuid\" = ?)" [uuid]]))

(defn insert-genome-reference
"This function adds a genome-reference to the database taking only a json object containing the genome name and the path." [body]
(log/warn "Object Passed ="(str body))
(exec-raw ["INSERT INTO \"genome_reference\" (uuid, name, path, created_by) VALUES
           ('7FB992E8-ED8C-458D-B49D-1C58E2CA1F9B', 'test', 'test', 0);"]))
