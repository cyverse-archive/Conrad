(ns conrad.genome-reference
  (:use
    [conrad.kormadb]
    [conrad.config]
    [clojure.data.json :only (json-str)]
    [korma.core]
    [korma.db]
    [kameleon.entities])
  (:require
    [clojure.tools.logging :as log]
    [clojure-commons.json :as cc-json])
  (:import
    [java.sql Timestamp]
    [java.util Date]))

;----------------------------Helper Functions----------------------------------

(defn get-name
  "This function takes in a userID and performs an sql query getting out the
  username, and returning it."
  [id]
  (:username (first (select users
                        (fields :username)
                        (where {:id id})))))

(defn get-id
  "This function takes in a username and performs an sql query which finds the
  users id, and returns it."
  [username]
  (:id (first (select users
                  (fields :id)
                  (where {:username username})))))

(defn get-full-username
  "this function appends the domain set in the config.clj to the request mapped
  username."
  [attrs]
  (str (attrs "uid") "@" (uid-domain)))

(defn get-or-create-id
  "This function will call get-id and if it returns nil it will create a new
  user and return that id, otherwise it will just return the id."
  [attrs]
  (let [username (get-full-username attrs)
        id       (get-id username)]
      (if (nil? id)
          (do (insert users (values {:username username}))
              (get-id username))
          id)))

(defn format-json-output
  "This function takes in a sequence and parses numbers into strings, nil
  values into empty strings, and Timestamp objects into epoch seconds into
  strings. For JSON encoding compliance."
  [data]
  (json-str {:genomes
      (mapv
          #(assoc %
              :created_by       (or (get-name (:created_by %)) "")
              :last_modified_by (or (get-name (:last_modified_by %)) "")
              :created_on       (str (.getTime (:created_on %)))
              :id               (str (:id %))
              :last_modified_on (if (:last_modified_on %)
                                    (str (.getTime (:last_modified_on %)))
                                    ""))
          data)}))

(defn uuid-gen
  "This function helps new genome reference record insertion by autogenerating
  a compliant Universal Unique ID."
  []
  (str (java.util.UUID/randomUUID)))

;----------------------Conrad.core Called Functions----------------------------

(defn get-all-genome-references
  "This function returns a JSON representation of the map of all the
  genome_reference table data, including 'deleted' records."
  []
  (format-json-output
      (select genome_reference)))

(defn get-genome-references
  "This function returns a JSON representation of the map of all the
  genome_reference table data, skipping 'deleted' records."
  []
  (format-json-output
      (select genome_reference
          (where {:deleted false}))))

(defn get-genome-references-by-username
  "This function returns a JSON representation the genome_reference table data
  in the DB that was created by the passed username, skips 'deleted' records."
  [username]
  (log/debug "Username Passed =" username)
  (format-json-output
      (select genome_reference
          (join users (= :users.id :genome_reference.created_by))
          (where {:users.username username :deleted false}))))

(defn get-genome-reference-by-uuid
  "This function returns a JSON representation of the genome_reference
  specified by the passed uuid, skips 'deleted' records."
  [id]
  (format-json-output
      (select genome_reference
          (where {:uuid id :deleted false}))))

(defn delete-genome-reference-by-uuid
  "This function updates the deleted column of the genome_reference that
  matches the passed UUID's genome_reference table to true, 'deleting' the
  reference."
  [uuid]
  (log/debug "UUID Passed =" uuid)
  (update genome_reference
      (set-fields {:deleted true})
      (where {:uuid uuid})))

(defn insert-genome-reference
  "This function adds a genome-reference to the database taking a JSON object
  containing the genome name and the path. The uuid is generated automatically,
  and the created_by info is pulled from the CAS info of the request map."
  [body attrs]
  (let [data       (cc-json/body->json body)
        uuid       (uuid-gen)
        name       (:name data)
        path       (:path data)
        created_by (get-or-create-id attrs)]
      (log/debug "JSON Object Passed=" data)
      (insert genome_reference
          (values [{:uuid       uuid
                    :name       name
                    :path       path
                    :created_by created_by}]))))

(defn modify-genome-reference
  "This function modifies an existing genome-reference in the database. It
  takes a JSON object containing the new genome name and path, and the existing
  UUID to identify it."
  [body attrs]
  (let [data             (cc-json/body->json body)
        uuid             (:uuid data)
        name             (:name data)
        path             (:path data)
        last_modified_by (get-or-create-id attrs)
        last_modified_on (Timestamp. (.getTime (Date.)))]
      (log/debug "JSON Object Passed=" data)
      (update genome_reference
          (where {:uuid uuid})
          (set-fields {:name             name
                       :path             path
                       :last_modified_by last_modified_by
                       :last_modified_on last_modified_on}))))
