(ns org.boteval.nlueval.evaluators.accuracy
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union intersection]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]))

(def Godbole-accuracy

  " prescribes calculation of accuracy at n per object, as per Godbole & Sarawagi, 2004.
    this entails caculating metrics by measures of 'overlap' for each object, and
    finally normalizing.

    see also http://lpis.csd.auth.gr/publications/tsoumakas-ijdwm.pdf
    and https://www.wikiwand.com/en/Precision_and_recall#/Precision "

  { :mapper (fn mapper [gold-tags test-tags _]
       (let
          [intersection-set (intersection gold-tags test-tags); the correctly predicted
           union-set (union gold-tags test-tags)

           correct-vs-gold ; for recall summation
           (divide-or-default
             (count intersection-set)
             (count gold-tags)
             1) ;; default to voidly perfect recall if nothing to recall for the object

           correct-vs-predicted ; for precision summation
           (divide-or-default
             (count intersection-set)
             (count test-tags)
             1) ; default to voidly perfect precision if no predictions made for the object

           intersection-vs-union ; for accuracy summation
           (divide-or-default
             (count intersection-set)
             (count union-set)
             1)] ; default to perfect accuracy if no predictions nor gold tags for the object

          { :correct-vs-gold correct-vs-gold
            :correct-vs-predicted correct-vs-predicted
            :intersection-vs-union intersection-vs-union}))

   :reducer (fn reducer [row-evaluations]
      (let
        [recall
            (divide-or-undef
              (apply + (map :correct-vs-gold row-evaluations))
              (count row-evaluations))

          precision
            (divide-or-undef
              (apply + (map :correct-vs-predicted row-evaluations))
              (count row-evaluations))

          accuracy
            (divide-or-undef
              (apply + (map :intersection-vs-union row-evaluations))
              (count row-evaluations))]

        { :recall recall
          :precision precision
          :accuracy accuracy })) })
