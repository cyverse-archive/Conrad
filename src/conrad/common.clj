(ns conrad.common
  (:use [clojure.data.json :only (json-str)]))

(def json-content-type "application/json")

(defn success-response [map]
  {:status 200
   :body (json-str (merge {:success true} map))
   :content-type json-content-type})

(defn failure-response [e]
  {:status 400
   :body (json-str {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn error-response [e]
  {:status 500
   :body (json-str {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn unrecognized-path-response []
  (let [msg "unrecognized service path"]
    (json-str {:success false :reason msg})))
