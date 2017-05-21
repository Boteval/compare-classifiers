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
  " helper returning the supplied value, or the supplied default value if the value is an empty string, or the string 'none'.
    this is currently an input-convention. "
  (if (or (= value "") (= value "none"))
    default
    value))

(defn ^:private get-canonical-for-row
  [object-id-mapping tagging-group-mappings tagging-group-name row]
  {:pre [(keyword? object-id-mapping)]}

   " derives a canonical tagging for the given row, for the requested result set name.
     each row is assumed to describe a single object for classification.

     normalizations performed here:
       + confidence scores are normalized to the [0,1] range
       + missing confidence scores are transformed to a confidence of 1
       + null tags are discarded "

   (let [result-set-mapping (val (first (first (filter #(= tagging-group-name (key (first %))) tagging-group-mappings))))]
     (let [raw-result-set
             (hash-map

               :tagging-group-name tagging-group-name

               :object-id (object-id-mapping row)

               :object-data-origin (:data-group row) ; TODO: sticking this here is the number one hack of this code base.
                                                     ;       to clean this up, make the objects-tagging collection a
                                                     ;       collection where each element is a keyed map, storing
                                                     ;       this bit of information at the object level there.

               :taggings
                 (map
                   (fn [tag-map]
                     (apply hash-map
                            (mapcat
                              (fn [[canonical-tag header]]
                                [canonical-tag
                                 (case canonical-tag

                                   :tag (let
                                          [value (get row header)]
                                          (value-or-default value nil))

                                   :confidence (let
                                                 [value
                                                  (if (keyword? header)
                                                    (get row header)
                                                    header)]
                                                 (as-float (value-or-default value 1)))

                                   :confidence-scale (as-float header)) ])

                              tag-map)))
                   result-set-mapping))]

           (update raw-result-set :taggings
             (fn filter-and-normalize [taggings]
               (map
                 (fn [tagging] ; normalizing confidence by the confidence scale of the tuple
                   (update
                      tagging :confidence
                      #(/ % (:confidence-scale tagging))))

                 (filter #(some? (:tag %)) taggings) ; filtering out nil taggings
              ))))))


(defn get-canonical-tagging [{:keys [data object-id-mapping tagging-group-mappings]} tagging-group-name]
  " get taggings per object-id, for the given tagging group name "
  (map
    (partial get-canonical-for-row
               object-id-mapping
               tagging-group-mappings
               tagging-group-name)
    data))


(defn get-tag-set [tagging]
  " extract the unique set of tags included in the provided tagging "
  (set (map #(:tag %) tagging)))
