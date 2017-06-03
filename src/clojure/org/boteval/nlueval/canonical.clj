(ns org.boteval.nlueval.canonical
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]))

(defn ^:private value-or-default [value default]
  " helper returning the supplied value, or the supplied default value if the value is an empty string.
    useful for translating to nils from CSV input. "
  (if (= value "")
    default
    value))

(defn ^:private get-canonical-for-row
  [accumulate-filtered-out
   valid-classes-set
   object-id-mapping
   tagging-group-mappings
   tagging-group-name
   row]
  {:pre [(keyword? object-id-mapping)]}

   " derives a canonical tagging for the given row, for the requested result set name.
     each row is assumed to describe a single object for classification.

     normalizations performed here:
       + confidence scores are normalized to the [0,1] range
       + missing confidence scores are transformed to a confidence of 1
       + null tags are discarded "

  (let
    [filtered-get-tag-value (fn
       [value allowed-classes]
       {:pre [(set? allowed-classes)]}

       " filters away tags outside the allowed tag list "

       (if (contains? allowed-classes value)
          value
          (do
             (if-not (= value nil) (accumulate-filtered-out value))
             nil)))

     result-set-mapping
       (val (first (first (filter #(= tagging-group-name (key (first %))) tagging-group-mappings))))

     raw-result-set
       (hash-map

         :tagging-group-name tagging-group-name

         :object-id (object-id-mapping row)

         :object-data-origin (:data-group row) ; TODO: sticking this here is the number one hack of this code base.
         ;       to clean this up, make the objects-tagging collection a
         ;       collection where each element is a keyed map, storing
         ;       this bit of information at the object level there.

         :taggings
           (doall (map
             (fn [tag-map]
               (apply hash-map
                      (mapcat
                        (fn [[canonical-tag header]]
                          [canonical-tag
                           (case canonical-tag

                             :tag (let
                                    [value (get row header)]
                                    (let [value (value-or-default value nil)]
                                      (if valid-classes-set
                                        (filtered-get-tag-value value valid-classes-set)
                                        value)))

                             :confidence (let
                                           [value
                                            (if (keyword? header)
                                              (get row header)
                                              header)]
                                           (as-float (value-or-default value 1)))

                             :confidence-scale (as-float header)) ])

                        tag-map)))
             result-set-mapping)))]

    (update raw-result-set :taggings
       (fn filter-and-normalize [taggings]
          (map
              (fn [tagging] ; normalizing confidence by the confidence scale of the tuple
                  (update
                    tagging :confidence
                    #(/ % (:confidence-scale tagging))))

                (filter #(some? (:tag %)) taggings) ; filtering out nil taggings!!
                )))))


(defn get-canonical-tagging
  [{:keys [data object-id-mapping valid-classes-set tagging-group-mappings]} tagging-group-name]
  " get taggings per object-id, for the given tagging group name "
  (let
    [filtered-out-tags (atom (list))
     accumulate-filtered-out
       (fn [value]  ; we accumulate as a "side-effect" here rather than rewrite it all
          (swap! filtered-out-tags conj value))

     result
       (doall (map
         (partial get-canonical-for-row
                    accumulate-filtered-out
                    valid-classes-set
                    object-id-mapping
                    tagging-group-mappings
                    tagging-group-name)
         data))]


    (if (not-empty @filtered-out-tags) (do
      (println (count @filtered-out-tags ) "tags outside the allowed tag list are found in tagging set" tagging-group-name "and will be ignored:")
      (cprint (frequencies @filtered-out-tags))))

    result))

(defn get-tag-set [tagging]
  " extract the unique set of tags included in the provided tagging "
  #_(inspect/inspect-tree (doall (map :tag tagging)))
  (set (map :tag tagging)))
