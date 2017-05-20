(ns org.boteval.nlueval.dimensional-evaluation
  (:require
    [clojure.pprint :refer [pprint]]
    [puget.printer :refer [cprint]]
    [clojure.inspector :as inspect :refer [inspect-tree]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
    [clojure.math.combinatorics :as combo]
    [org.boteval.nlueval.input :refer :all]
    [org.boteval.nlueval.canonical :refer :all]
    [org.boteval.nlueval.accuracy :refer :all]))


(defn evaluate-all-dimensions [dimensions execution-config-base]

  " drive evaluation over multiple provided dimensions "

  (let
    ;; cartesian product describing
    [evaluation-combos
      (apply combo/cartesian-product
         (map
            (fn [dim]
              (map
                (fn [dim-val]

                  { ; function to apply this dimension's value to the evaluation being dispatched
                    :evaluation-config-transform (partial (:evaluation-config-transform dim) dim-val)

                    ; data for recording this dimensions value aside the result of its evaluation
                    :column-data
                      { (:name dim)
                        dim-val }})

                (:vals dim)))
            dimensions))]

      (map
        (fn configure-and-execute [evaluation-combo]
           (let
             [evaluation-config
                (reduce
                  (fn apply-args-transform [config dim-action-struct]
                    ((:evaluation-config-transform dim-action-struct) config))
                  execution-config-base
                  evaluation-combo)

              evaluation-result (get-accuracy-at evaluation-config)

              dimensions-column-data
                 (map
                   #(:column-data %)
                   evaluation-combo)]

              (apply merge evaluation-result dimensions-column-data)))

        evaluation-combos)))

