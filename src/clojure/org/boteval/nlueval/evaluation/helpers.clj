(ns org.boteval.nlueval.evaluation.helpers
  (:require
      [org.boteval.nlueval.util :refer :all]
      [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
      [clojure.data.csv :as csv]
      [clojure.java.io :as io]
      [clojure.set :refer [union]]
      [clojure.pprint :refer [pprint]]
      [puget.printer :refer [cprint]]
      [clojure.inspector :as inspect :refer [inspect-tree]]))


(defn get-row-tags

  " a helper for extracting the gold tags and test tags from the provided record "

  [{:keys [gold test-tagging-group-name n]} object-tagging]
  {:pre
   [(keyword? gold)
    (keyword? test-tagging-group-name)
    (number? n)]}

  (let [object-id (key object-tagging)
        taggings-groups (val object-tagging)

        gold-taggings (:taggings (get-single #(map-key-equals % :tagging-group-name gold) taggings-groups))
        gold-tags (set (map :tag gold-taggings))

        test-taggings (:taggings (get-single #(map-key-equals % :tagging-group-name test-tagging-group-name) taggings-groups))
        test-tags (set (map :tag (take n test-taggings)))]

    (to-map object-id gold-tags test-tags)))

