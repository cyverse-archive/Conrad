(ns conrad.config
  (:use [clojure.string :only (blank?)])
  (:require [clojure-commons.props :as cc-props]
            [clojure.tools.logging :as log]))

(def
  ^{:doc "The name of the properties file."}
  prop-file "conrad.properties")

(def
  ^{:doc "The properties loaded from the properties file."}
   zk-props (cc-props/parse-properties prop-file))

(def
  ^{:doc "The URL used to connect to zookeeper."}
   zk-url (get zk-props "zookeeper"))

(def
  ^{:doc "The properites that have been loaded from Zookeeper."}
   props (atom nil))

(def
  ^{:doc "The list of required properties."}
   required-props (ref []))

(def
  ^{:doc "True if the configuraiton is valid."}
   configuration-is-valid (atom true))

(defn- record-missing-prop
  "Records a property that is missing.  Instead of failing on the first
   missing parameter, we log the missing parameter, mark the configuration
   as invalid and keep going so that we can log as many configuration errors
   as possible in one run."
  [prop-name]
  (log/error "required configuration setting" prop-name "is empty or"
             "undefined")
  (reset! configuration-is-valid false))

(defn- record-invalid-prop
  "Records a property that is invalid.  Instead of failing on the first
   invalid parameter, we log the parameter name, mark the configuraiton as
   invalid and keep going so that we can log as many configuration errors as
   possible in one run."
  [prop-name t]
  (log/error "invalid configuration setting for" prop-name ":" t)
  (reset! configuration-is-valid false))

(defn- get-str
  "Gets a string property from the properties that were loaded from
   Zookeeper."
  [prop-name]
  (let [value (get @props prop-name)]
    (log/trace prop-name "=" value)
    (when (blank? value)
      (record-missing-prop prop-name))
    value))

(defn- get-int
  "Gets an integer property from the properties that were loaded from
   Zookeeper."
  [prop-name]
  (try
    (Integer/valueOf (get-str prop-name))
    (catch NumberFormatException e
      (do (record-invalid-prop prop-name e) 0))))

(defmacro defprop
  "Defines a property."
  [sym docstr & init-forms]
  `(def ~(with-meta sym {:doc docstr}) (memoize (fn [] ~@init-forms))))

(defn- required
  "Registers a property in the list of required properties."
  [prop]
  (dosync (alter required-props conj prop)))

(required
  (defprop listen-port
    "The port to listen to for incoming connections."
    (get-int "conrad.listen-port")))

(required
  (defprop db-vendor
    "The name of the database vendor (e.g. postgresql)."
    (get-str "conrad.db.vendor")))

(required
  (defprop db-host
    "the host name or IP address used to connect to the database."
    (get-str "conrad.db.host")))

(required
  (defprop db-port
    "The port used to connect to the database."
    (get-str "conrad.db.port")))

(required
  (defprop db-name
    "The name of the database."
    (get-str "conrad.db.name")))

(required
  (defprop db-user
    "The username used to authenticate to the databse."
    (get-str "conrad.db.user")))

(required
  (defprop db-password
    "The password used to authenticate to the database."
    (get-str "conrad.db.password")))

(required
  (defprop db-max-idle-time
    "The maximum amount of time to retain idle database connections."
    (* (get-int "conrad.db.max-idle-minutes") 60)))

(required
  (defprop cas-server
    "The URL prefix to use when connecting to the CAS server."
    (get-str "conrad.cas.server")))

(required
  (defprop server-name
    "The name of the local server to provide to CAS."
    (get-str "conrad.server-name")))

(defn configuration-valid
  "Ensures that all required properties are valued."
  []
  (dorun (map #(%) @required-props))
  @configuration-is-valid)
