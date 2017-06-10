(ns org.boteval.nlueval.evaluators.map-reduce-evaluate
  (:require
      [org.boteval.nlueval.util :refer :all]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]
      [org.boteval.nlueval.evaluators.helpers :refer :all]))

(defn map-reduce-evaluate [{:keys [mapper reducer]} evaluation-config]
  {:pre [(is-function? mapper) (is-function? reducer)]}

  " applies an evaluator "

  (letfn
    [(do-mapper [evaluation-config object-tagging]
       (let
          [{:keys [object-id gold-tags test-tags]} (get-row-tags evaluation-config object-tagging)]
          (merge
            (mapper gold-tags test-tags evaluation-config)
            { :object-id object-id
              :gold-tags gold-tags
              :test-tags test-tags })))]

  (let
     [mapped
       (map
         (partial do-mapper (dissoc evaluation-config :objects-tagging))
         (:objects-tagging evaluation-config))

      result (reducer mapped)]

     #_(println (count mapped))
     #_(println (keys result))
     #_(cprint (first mapped))
     #_(inspect/inspect-tree (first (reducer mapped)))

     { :trace mapped
       :result result })))
