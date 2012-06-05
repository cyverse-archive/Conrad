(ns conrad.genome-reference
  (:use [conrad.kormadb]
        [conrad.config]
        [clojure.data.json :only (json-str)]
        [korma.core]
        [korma.db]
        [kameleon.entities])
  (:require [clojure.tools.logging :as log]))

(defn get-genome-references
"This function returns a JSON representation of the map of all the genome_reference table data in the DB (for testing purposes)" []
(json-str (str (select genome_reference))))

(defn get-genome-references-by-username
"This function returns a map of all the genome_reference table data in the DB with the passed id." [username] (log/warn "Username Passed ="username)
(json-str(str (select genome_reference (join users (= :users.id :genome_reference.created_by)) (where {:users.username username})))))
