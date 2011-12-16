(ns conrad.app-admin
  (:use [clojure.data.json :only (json-str)]
        [conrad.database])
  (:require [clojure.java.jdbc :as jdbc]))

(defn update-app [app-listing]
  app-listing)
