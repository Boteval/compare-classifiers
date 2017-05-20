(ns org.boteval.nlueval.error
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]))


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
