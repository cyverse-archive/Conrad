(ns conrad.database
  (:use [conrad.config])
  (:import javax.sql.DataSource
           com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- get-required [props name msg]
  (let [value (get props name)]
    (if (nil? value)
      (throw (IllegalArgumentException. msg))
      value)))

(def drivers
  {"mysql" "com.mysql.jdbc.Driver"
   "postgresql" "org.postgresql.Driver"})

(defn- get-driver [vendor]
  (get-required drivers vendor (str "driver not known for " vendor)))

(def subprotocols
  {"mysql" "mysql"
   "postgresql" "postgresql"})

(defn- get-subprotocol [vendor]
  (get-required subprotocols vendor (str "subprotocol not known for " vendor)))

(def db-spec
  {:classname (get-driver db-vendor)
   :subprotocol (get-subprotocol db-vendor)
   :subname (str "//" db-host ":" db-port "/" db-name)
   :user db-user
   :password db-password
   :max-idle-time db-max-idle-time})

(defn- pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMaxIdleTime(:max-idle-time spec)))]
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))

(defn db-connection [] @pooled-db)
