(ns conrad.app-listings
  (:use [conrad.app-crud])
  (:require [clojure.tools.logging :as log]))

(defn- step-count-msg [id, adj]
  (str "analysis, " id ", has too " adj " steps for a pipeline"))

(defn- app-pipeline-eligibility [app]
  (let [step-count (:step_count app) id (:id app)]
    (cond
     (< step-count 1) {:is_valid false :reason (step-count-msg id "few")}
     (> step-count 1) {:is_valid false :reason (step-count-msg id "many")}
     :else {:is_valid true :reason ""})))

(defn- normalize-deployed-component-listing [dc]
  {:id (:deployed_component_id dc)
   :name (:name dc)
   :description (:description dc)
   :location (:location dc)
   :type (:type dc)
   :version (:version dc)
   :attribution (:attribution dc)})

(defn- app-deployed-component-listing [app]
  (map #(normalize-deployed-component-listing %)
       (load-deployed-components-for-app (:hid app))))

(defn- normalize-app-listing [app]
  {:id (:id app)
   :name (:name app)
   :description (:description app)
   :integrator_email (:integrator_email app)
   :integrator_name (:integrator_name app)
   :rating {:average (:average_rating app)}
   :is_public (:is_public app)
   :is_favorite false
   :wiki_url (:wikiurl app)
   :deployed_components (app-deployed-component-listing app)
   :pipeline_eligibility (app-pipeline-eligibility app)
   :suggested_categories (load-suggested-categories-for-app (:hid app))})

(defn load-app-listing [hid]
  (normalize-app-listing (list-app hid)))
