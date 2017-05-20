(ns org.boteval.nlueval.accuracy
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]))


(defn get-accuracy-at
  [{:keys
    [objects-tagging
     gold
     test-tagging-group-name
     test-tag
     n]}]

  {:pre [(keyword? gold)
         (keyword? test-tagging-group-name)]
         (keyword? test-tag)
         (number? n)}

  " calculates accuracy at n, over the provided objects taggging collection
    see https://www.wikiwand.com/en/Precision_and_recall#/Precision "

  (letfn
    [(row-evaluation
       [object-tagging]
           (let [object-id (key object-tagging)
                 taggings-groups (val object-tagging)

                 gold-taggings (:taggings (get-single #(val=! % :tagging-group-name gold) taggings-groups))
                 gold-tags (set (map #(:tag %) gold-taggings))

                 test-taggings (:taggings (get-single #(val=! % :tagging-group-name test-tagging-group-name) taggings-groups))
                 test-tags (set (map #(:tag %) (take n test-taggings)))

                 positive?  (contains? gold-tags test-tag)
                 predicted? (contains? test-tags test-tag)
                 true-positive?  (and positive? predicted?)
                 false-positive? (and (not positive?) predicted?)]

             { :positive? positive?
               :predicted? predicted?
               :true-positive? true-positive?
               :false-positive? false-positive? }))]

      (let
        [row-evaluations (map row-evaluation objects-tagging)

         positives (count (filter #(:positive? %) row-evaluations))
         true-positives (count (filter #(:true-positive? %) row-evaluations))
         false-positives (count (filter #(:false-positive? %) row-evaluations))]

          { :positives positives
            :true-positives true-positives
            :false-positives false-positives

            :precision (undef-or-divide
                          true-positives
                          (+ true-positives false-positives))

            :recall (undef-or-divide
                       true-positives
                       positives) }
        )))



(defn get-error [gold test penalty-modifier-fn]
  " calculates the aggregate error between gold and test,
    applies the provided penalty function to each error distance "
  (letfn [(row-error
            [gold test]
            (let [gold-taggings (val (first gold))
                  test-taggings (val (first test))
                  gold-tags (set (map #(:tag %) gold-taggings))
                  test-tags (set (map #(:tag %) test-taggings))
                  union (union gold-tags test-tags)]

              (apply + (map (fn tag-error [tag]
                      (let [gold (first (filter #(= (:tag %) tag) gold-taggings))
                            test (first (filter #(= (:tag %) tag) test-taggings))]
                            ;(println gold-taggings)
                            ;(println test-taggings)
                            ;(println "gold" gold)
                            ;(println "test" test)
                            ;(println (vec [(some? gold) (some? test)]))
                         (let [error-cost
                           (penalty-modifier-fn
                             (case (vec [(some? gold) (some? test)])
                               [true true]   (- (:confidence gold) (:confidence test))
                               [true false]  (:confidence gold)
                               [false true]  (:confidence test)
                               [false false] 0))]
                              ;(println error-cost)
                              error-cost))) union))))]

  (/ (apply + (map row-error gold test))
     (count gold))))
