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

(defn get-name
  "This function takes in a userID and performs an sql query which finds the username, and returns it."
  [id]
  (:username (first (select users
                      (fields :username)
                      (where {:id id})))))

(defn get-id
  "This function takes in a username and performs an sql query which finds the user_id, and returns it."
  [username]
  (:id (first (select users
                (fields :id)
                (where {:username username})))))

(defn json-parser
  "This function takes in a sequence and parses non-strings into strings, nil values into empty strings, and Timestamp objects into epoch seconds for JSON encoding compliance."
  [body]
  {:genomes (mapv #(assoc % :created_by (or (get-name (:created_by %)) "")
                            :last_modified_by (or (get-name (:last_modified_by %)) "")
                            :created_on (str (.getTime (:created_on %)))
                            :id (str (:id %))
                            :last_modified_on (or (:last_modified_on %) "")) body)})

(defn uuid-gen
  "Auto generates a Unique Universal ID for new genome reference records."
  []
  (str (java.util.UUID/randomUUID)))

;-----------------------------Conrad.core Called Functions----------------------------------

(defn get-all-genome-references
  "This function returns a JSON representation of the map of all the genome_reference table data, including 'deleted' records."
  []
  (json-str (json-parser (select genome_reference))))

(defn get-genome-references
  "This function returns a JSON representation of the map of all the genome_reference table data, skipping 'deleted' records."
  []
  (json-str (json-parser (select genome_reference (where {:deleted false})))))

(defn get-genome-references-by-username
  "This function returns a JSON representation of the map of all the genome_reference table data in the DB that was created by the passed username."
  [username]
  (log/warn "Username Passed =" username)
  (json-str (json-parser (select genome_reference
                            (join users (= :users.id :genome_reference.created_by))
                            (where {:users.username username :deleted false})))))

(defn delete-genome-references-by-UUID
  "This function updates the deleted column of the genome_reference that matches the passed UUID's genome_reference table to true."
  [uuid]
  (log/warn "UUID Passed =" uuid)
  (update genome_reference(set-fields {:deleted true})(where {:uuid uuid})))

(defn insert-genome-reference
  "This function adds a genome-reference to the database taking a JSON object containing the genome name and the path. The uuid is generated automatically, and the created_by info is pulled from the CAS request map."
  [body attrs]
  (def data (cc-json/body->json body))
  (log/warn "JSON Object Passed=" data)
  (let [uuid (uuid-gen) name (:name data) path (:path data) cb (get-id (get-in attrs ["uid"]))]
    (insert genome_reference (values [{:uuid uuid :name name :path path :created_by cb}]))))

  (defn modify-genome-reference
  "This function modifies an existing genome-reference in the database. It takes a JSON object containing the new genome name and path, and the existing UUID as a reference."
  [body attrs]
  (def data (cc-json/body->json body))
  (log/warn "JSON Object Passed=" data)
  (let [uuid (:uuid data) name (:name data) cb (get-id (get-in attrs ["uid"])) path (:path data)]
    (update genome_reference
      (where {:created_by cb :uuid uuid})
      (set-fields {:name name :path path}))))
