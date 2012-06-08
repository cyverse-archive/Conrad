(ns conrad.genome-reference
  (:use
    [conrad.kormadb]
    [clojure.data.json :only (json-str)]
    [korma.core]
    [korma.db]
    [kameleon.entities])
  (:require
    [clojure.tools.logging :as log]
    [clojure-commons.json :as cc-json])
  (:import
    [java.sql Timestamp]))

;---------------------------------Helper Functions------------------------------------------
;
;(defn json-parser
;  "This function coerces the timestamp field returned by the database into a JSON encode friendl format."
;  [input]
;  (str {:timestamp input}))

;(defn json-slurper
;  "This function converts the body of the passed object into a json compliant object."
;  [body]
;  (cc-json/body->json body))

;-----------------------------Conrad.core Called Functions----------------------------------

(defn get-genome-references
  "This function returns a JSON representation of the map of all the genome_reference table data in the DB (for testing purposes)"
  []
  (json-str (str (select genome_reference))))

(defn get-genome-references-by-username
  "This function returns a JSON representation of the map of all the genome_reference table data in the DB that was created by the passed username."
  [username]
  (log/warn "Username Passed =" username)
  (json-str (str (select genome_reference
                    (join users (= :users.id :genome_reference.created_by))
                    (where {:users.username username})))))

(defn delete-genome-references-by-UUID
  "This function updates the deleted column of the genome_reference that matches the passed UUID's genome_reference table to true."
  [uuid]
  (log/warn "UUID Passed =" uuid)
  (exec-raw ["UPDATE \"genome_reference\" SET \"deleted\" = TRUE
              WHERE (\"genome_reference\".\"uuid\" = ?)" [uuid]]))

(defn insert-genome-reference
  "This function adds a genome-reference to the database taking a JSON object containing the genome name and the path. TODO: uuid and created_by autogeneration should be generated sans JSON."
  [body]
  (def data (cc-json/body->json body))
  (log/warn "JSON Object Passed=" data)
  (let [uuid (:uuid data) name (:name data) path (:path data) cb (:created_by data)]
    (exec-raw ["INSERT INTO \"genome_reference\" (uuid, name, path, created_by)
                VALUES (?, ?, ?, ?);" [uuid name path cb]])))

;-----------------------------Non-Functional Korma Code-------------------------------------
;insert-genome-reference
;(insert genome_reference (values [{:uuid uuid :name name :path path :created_by cb}])))
;
;delete-genome-reference
;(update genome_reference(set-fields {:deleted true})(where {:uuid uuid}))
;-------------------------------------------------------------------------------------------
