(ns org.boteval.nlueval.evaluators.per-tag
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [org.boteval.nlueval.evaluators.helpers :refer :all]
      [clojure.inspector :as inspect :refer [inspect-tree]]))


(def accuracy-at

  " prescribes calculation of simple accuracy at n for the provided test-tag, over the provided objects

    see also https://www.wikiwand.com/en/Precision_and_recall#/Precision "

  { :mapper (fn mapper [gold-tags test-tags {:keys [test-tag]}]
       (let
         [positive?  (contains? gold-tags test-tag)
          predicted? (contains? test-tags test-tag)
          true-positive?  (and positive? predicted?)
          false-positive? (and (not positive?) predicted?)]

         { :positive? positive?
           :predicted? predicted?
           :true-positive? true-positive?
           :false-positive? false-positive? }))

    :reducer (fn reducer [row-evaluations]
      (let
        [positives (count (filter :positive? row-evaluations))
         true-positives (count (filter :true-positive? row-evaluations))
         false-positives (count (filter :false-positive? row-evaluations))

         precision (divide-or-undef
                      true-positives
                      (+ true-positives false-positives))

         recall (divide-or-undef
                   true-positives
                   positives)

         F1 (if
              (and (number? precision) (number? recall))
                (divide-or-undef
                   (* 2 precision recall)
                   (+ precision recall))
                :undef)]

        { :support positives
          :true-positives true-positives
          :false-positives false-positives
          :precision precision
          :recall recall
          :F1 F1 }
        ))})
