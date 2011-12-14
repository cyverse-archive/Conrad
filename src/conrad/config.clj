(ns conrad.config
  (:require [clojure-commons.props :as cc-props]))

(defn- get-required [props name msg]
  (let [value (get props name)]
    (if (nil? value)
      (throw (IllegalStateException. msg))
      value)))

(def prop-file "conrad.properties")

(def config-props (cc-props/parse-properties prop-file))

(defn- req [name]
  (let [msg (str "property, " name ", not defined in " prop-file)]
    (get-required config-props name msg)))

(def db-vendor (req "conrad.db.vendor"))
(def db-host (req "conrad.db.host"))
(def db-port (req "conrad.db.port"))
(def db-name (req "conrad.db.name"))
(def db-user (req "conrad.db.user"))
(def db-password (req "conrad.db.password"))
(def db-max-idle-time (* (Integer/valueOf (req "conrad.db.max-idle-minutes")) 60))
