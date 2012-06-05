(ns conrad.genome-reference
  (:use [conrad.kormadb]
        [conrad.config]
        [korma.core]
        [korma.db])
  (:require [clojure.tools.logging :as log]))

(defn get-genome-references
"This function returns a map of all the genome_reference table data in the DB (for testing purposes)" []
(str (select "genome_reference")))
