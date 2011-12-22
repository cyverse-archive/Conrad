(ns conrad.listings
  (:use [clojure.data.json :only (json-str)]
        [conrad.category-listings]
        [conrad.database])
  (:require [clojure.java.jdbc :as jdbc]))

(defn get-public-categories []
  (jdbc/with-connection (db-connection)
    (json-str (list-public-categories-without-apps))))

(defn get-category-with-apps [category-id]
  (jdbc/with-connection (db-connection)
    (json-str (list-category-with-apps category-id))))
