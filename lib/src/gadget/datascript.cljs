(ns gadget.datascript
  (:require [clojure.core.protocols :refer [Datafiable]]
            [datascript.core :as d]
            [datascript.db :as db]
            [datascript.impl.entity :as entity]
            [gadget.browsable :as browsable]
            [gadget.core :as gadget]
            [gadget.datafy :as datafy]
            [me.tonsky.persistent-sorted-set :as pss]))

(def db-key-order [:schema :eavt :aevt :avet :max-eid :max-tx :rschema :hash])

(extend-protocol browsable/Browsable
  pss/BTSet
  (entries [v] (map-indexed vector v))

  db/DB
  (entries [db]
    (conj (sort-by (gadget/key-order db-key-order) db)
          [:entities (->> (:eavt db)
                          (map first)
                          set
                          sort
                          (map #(d/entity db %)))])))

(extend-type entity/Entity
  Datafiable
  (datafy [e]
    (into {:db/id (:db/id e)} e)))

(datafy/add-type-inference
 (fn [v]
   (when (instance? db/Datom v)
     :datom)))

(datafy/add-type-inference
 (fn [v]
   (when (instance? entity/Entity v)
     :entity)))

(defmethod gadget/render [:inline :datom] [_ {:keys [raw label path]}]
  (if (gadget/too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (gadget/summarize "datom [" raw "]")]]
    [:gadget/inline-coll {:brackets ["datom [" "]"]
                          :xs (map #(gadget/render-with-view :summary label path %) raw)}]))

(defmethod gadget/render [:summary :entity] [_ {:keys [raw label path]}]
  [:gadget/inline-coll
   {:brackets ["{" "}"]
    :xs [(gadget/render-with-view :inline label path :db/id)
         " "
         (gadget/render-with-view :inline label (conj path :db/id) (:db/id raw))]}])

(defmethod gadget/render [:inline :entity] [_ v]
  (gadget/render :inline (-> v
                             (assoc :type :map)
                             (update :raw datafy/datafy))))
