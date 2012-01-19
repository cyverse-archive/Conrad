(ns conrad.middleware.cas-proxy-auth
  (:use [clojure.contrib.string :only (blank?)]
        [clojure.data.json :only (json-str)]
        [conrad.common :only (unauthorized-response)]
        [conrad.config])
  (:require [clojure.tools.logging :as log])
  (:import [org.jasig.cas.client.validation Cas20ProxyTicketValidator
            TicketValidationException]))

(defn- valid-proxy-ticket?
  "Determines whether or not the given proxy ticket is valid."
  [proxy-ticket]
  (if (not (blank? proxy-ticket))
    (let [validator (Cas20ProxyTicketValidator. cas-server)]
      (.setAcceptAnyProxy validator true)
      (try
        (do (.validate validator proxy-ticket server-name) true)
        (catch TicketValidationException e
          (do (log/error e "proxy ticket validation failed") false))))
    false))

(defn validate-cas-proxy-ticket
  "Authenticates a CAS proxy ticket that has been sent to the service in a
   query string parameter called, proxyToken.  If the proxy ticket can be
   validated then the request is passed to the handler.  Otherwise, the
   handler responds with HTTP status code 401.

   This handler currently relies upon two configuration parameters:
   conrad.cas.server and conrad.server-name."
  [handler]
  (fn [request]
    (let [proxy-ticket (get (:query-params request) "proxyToken")]
      (log/warn (str "validating proxy ticket: " proxy-ticket))
      (if (valid-proxy-ticket? proxy-ticket)
        (handler request)
        (unauthorized-response (:uri request))))))