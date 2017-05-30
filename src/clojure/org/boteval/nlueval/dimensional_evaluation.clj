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
    [org.boteval.nlueval.canonical :refer :all]))


(defn evaluate-on-dimensions [trace-writer {:keys [dimensions evaluation-fn evaluation-config-base]}]

  " drives the supplied evaluation function across multiple provided dimensions,
    iterating through the cartesian product of all supplied dimensions' values.
    at each evaluation, the arguments to the evaluation function are obtained from
    two sources: the supplied dimensions, and the evaluation config base.

    collectively, the evaluation config base, and the mappings
    included with each dimension, should enable this function
    passing all arguments required by the target evaluation function. "

  (let
    ;; cartesian product prescribing all runs for the given evlauation function
    [evaluation-combos
      (apply combo/cartesian-product
         (map
            (fn [dim]
              (map
                (fn [dim-val]

                  { ; function to apply this dimension's value to the evaluation being dispatched
                    :evaluation-config-transform (partial (:evaluation-config-transform dim) dim-val)

                    ; data for recording this dimension's value aside the result of its evaluation
                    :column-data
                      { (:name dim)
                        dim-val }})

                (:vals dim)))
            dimensions))]

      (doall (map
        (fn configure-and-execute [evaluation-combo]
           (let
             [evaluation-config
                (reduce
                  (fn apply-args-transform [config dim-action-struct]
                    ((:evaluation-config-transform dim-action-struct) config))
                  evaluation-config-base
                  evaluation-combo)

              evaluation
                (evaluation-fn evaluation-config)

              dimensions-column-data
                 (map
                   #(:column-data %)
                   evaluation-combo)]

              (trace-writer
                (apply merge (dissoc evaluation-config :objects-tagging :gold) dimensions-column-data)
                (:trace evaluation))

              ; merge each evaluation result row, with the dimensions
              ; that apply to it, to yield a fully descriptive data row
              (apply merge (:result evaluation) dimensions-column-data)))

        evaluation-combos))))
