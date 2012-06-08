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
;(log/warn "PASSED="(str (clojure-commons.json/body->json body)))
(def data (clojure-commons.json/body->json body))
(let [idpass (:uuid data) namepass (:name data) pathpass (:path data) cbpass (:created_by data)](str idpass ", " namepass ", " pathpass ", " cbpass)))
;(exec-raw ["INSERT INTO \"genome_reference\" (uuid, name, path, created_by) VALUES (\"idpass\", ;\"namepass\", \"pathpass\", \"cbpass\");"[idpass namepass pathpass cbpass]])))
;(insert genome_reference (values [{:uuid "9FB992E8-EB8C-458D-C49C-1C58E2CA1F9B" :name "TEST" :pa;th "/TEST/" :created_by 1}])))
