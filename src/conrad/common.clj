(ns conrad.common
  (:use [clojure.data.json :only (json-str)]
        [clojure.string :only (upper-case)])
  (:require [clojure.tools.logging :as log])
  (:import [java.util UUID]))

(def json-content-type "application/json")

(defn success-response [map]
  {:status 200
   :body (json-str (merge {:success true} map))
   :content-type json-content-type})

(defn failure-response [e]
  (log/error e "internal error")
  {:status 400
   :body (json-str {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn error-response [e]
  (log/error e "bad request")
  {:status 500
   :body (json-str {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn unrecognized-path-response []
  (let [msg "unrecognized service path"]
    (json-str {:success false :reason msg})))

(defn unauthorized-response [url]
  (log/warn (str "unauthorized request: " url))
  {:status 401
   :body (json-str {:success false :reason "UNAUTHORIZED"})})

(defn extract-required-field [obj field-name]
  (let [value (get obj field-name)]
    (if (nil? value)
      (throw (IllegalArgumentException.
              (str "missing required field, " field-name ", in request"))))
    value))

(defn uuid []
  (upper-case (str (java.util.UUID/randomUUID))))
