(ns conrad.genome-reference
  (:use [conrad.kormadb]
        [conrad.config]
        [clojure.data.json :only (json-str)]
        [korma.core]
        [korma.db])
  (:require [clojure.tools.logging :as log]))

(defn get-genome-references
"This function returns a JSON representation of the map of all the genome_reference table data in the DB (for testing purposes)" []
(json-str (select "genome_reference")))

(defn get-genome-reference-by-id
"This function returns a map of all the genome_reference table data in the DB (for testing purposes)" [id]
(json-str (select "genome_reference" (where {:id = id}))))
