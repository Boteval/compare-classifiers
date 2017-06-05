(ns org.boteval.nlueval.execute
  (:require
    [org.boteval.nlueval.util :refer :all]
    [clojure.pprint :refer [pprint]]
    [puget.printer :refer [cprint]]
    [clojure.inspector :as inspect :refer [inspect-tree]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
    [org.boteval.nlueval.input.ready :refer :all]
    [org.boteval.nlueval.output :refer :all]
    [org.boteval.nlueval.dimensional-evaluation :refer :all]
    [org.boteval.nlueval.evaluators.per-tag  :refer :all]
    [org.boteval.nlueval.evaluators.accuracy :refer :all]))


(defn ^:private filter-domain-ness [objects-tagging gold in-or-out]
  " filters to only objects which have at least one gold label to them ―
    which we aptly regard as inside the domain (of the classification task) v.s.
    objects which have no gold tags, and thus, considered outside of the domain
    of the classification. this is useful when the classification domain is
    only a subset of the overall input domain "
  (let
    [predicate
       (case in-or-out
         :in  not-empty
         :out empty?)

     filtered
       (filter
          (fn [object-tagging]
             (let
               [object-id (key object-tagging)
                taggings-groups (val object-tagging)
                gold-taggings (:taggings (get-single #(map-key-equals % :tagging-group-name gold) taggings-groups))
                gold-tags (set (map :tag gold-taggings))]

               (predicate gold-tags)))

          objects-tagging)]

    filtered))


(defn ^:private filter-data-origin-group [objects-tagging origin-name gold]
  " filters to only objects that belong to the given data group.
    this function will be radically simplified once :object-data-group
    finds a better home in the collection hierarchy "
  (let [filtered
     (filter
        (fn [object-tagging]
           (let
             [taggings-groups (val object-tagging)
              object-data-origin (:object-data-origin (get-single #(map-key-equals % :tagging-group-name gold) taggings-groups))]

             (= object-data-origin origin-name)))

        objects-tagging)]

    filtered))


(defn dims
  " build up our default dimensions "

   [{:keys
       [data
        gold
        gold-taggings
        classifiers-under-test
        target-tag-set
        objects-tagging]}]

        {:classifiers-dim
          {:name :classifier
           :vals classifiers-under-test
           :evaluation-config-transform
           (fn [current-dim-val evaluation-config] (assoc evaluation-config :test-tagging-group-name current-dim-val))}

         :tags-dim
           {:name :tag
            :vals target-tag-set
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] (assoc evaluation-config :test-tag current-dim-val))}

         :at-n-dim
           {:name :n
            :vals [1 2 3]
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] (assoc evaluation-config :n current-dim-val))}

         :in-out-domain
           {:name :domain-ness?
            :vals [:domain :exa-domain :all]
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] ;; must base on current to maintain commutativity
               (let [current (:objects-tagging evaluation-config)]
                 (assoc
                   evaluation-config
                   :objects-tagging (case current-dim-val
                      :domain (filter-domain-ness current gold :in)
                      :exa-domain (filter-domain-ness current gold :out)
                      :all current))))}

         :in-out-corpus
           {:name :origin?
            :vals [:corpus :exa-corpus :all]
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] ;; must base on current to maintain commutativity
               (let [current (:objects-tagging evaluation-config)]
                 (assoc
                   evaluation-config
                   :objects-tagging (case current-dim-val
                      :corpus     (filter-data-origin-group current :corpus gold)
                      :exa-corpus (filter-data-origin-group current :exa-corpus gold)
                      :all current))))}})




(defn execute
  [{:keys
    [in-out-corpus
     in-out-domain
     tags-dim
     at-n-dim
     classifiers-dim]}

   {:keys
    [objects-tagging
     gold]}]

  " drive evaluation on two evaluation methods sharing some of their dimensions "

     (write-evaluation-result
       "accuracy-at"
       (evaluate-on-dimensions
         (partial trace-write "accuracy-at")
         { :evaluation-fn accuracy-at
           :evaluation-config-base
           {:objects-tagging objects-tagging
            :gold gold}
           :dimensions
           [in-out-corpus
            in-out-domain
            tags-dim
            at-n-dim
            classifiers-dim]}))

     (write-evaluation-result
       "Godbole-accuracy"
       (evaluate-on-dimensions
         (partial trace-write "Godbole-accuracy")
         { :evaluation-fn Godbole-accuracy
           :evaluation-config-base
           {:objects-tagging objects-tagging
            :gold gold
            :n 3}
           :dimensions
           [in-out-corpus
            in-out-domain
            classifiers-dim]})))



