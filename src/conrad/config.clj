(ns conrad.config
  (:use [korma.db])
  (:require [clojure-commons.props :as cc-props]))

(def prop-file "conrad.properties")

(def props (cc-props/parse-properties prop-file))

(defdb db (postgres {:db (get props "conrad.db.name")
                     :host (get props "conrad.db.host")
                     :port (get props "conrad.db.port")
                     :user (get props "conrad.db.user")
                     :password (get props "conrad.db.password")}))

