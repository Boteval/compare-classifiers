(ns org.boteval.nlueval.execute
  (:require
    [org.boteval.nlueval.util :refer :all]
    [clojure.pprint :refer [pprint]]
    [puget.printer :refer [cprint]]
    [clojure.inspector :as inspect :refer [inspect-tree]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
    [org.boteval.nlueval.input :refer :all]
    [org.boteval.nlueval.output :refer :all]
    [org.boteval.nlueval.canonical :refer :all]
    [org.boteval.nlueval.dimensional-evaluation :refer :all]
    [org.boteval.nlueval.accuracy  :refer :all]
    [org.boteval.nlueval.accuracy2 :refer :all]))


(defn ^:private filter-in-domain [objects-tagging gold]
  " filters to only objects which have at least one gold label to them "
  (let [filtered
     (filter
        (fn [object-tagging]
           (let
             [object-id (key object-tagging)
              taggings-groups (val object-tagging)
              gold-taggings (:taggings (get-single #(val=! % :tagging-group-name gold) taggings-groups))
              gold-tags (set (map #(:tag %) gold-taggings))]

             (not-empty gold-tags)))

        objects-tagging)]

    filtered))


(defn ^:private filter-data-origin-group [objects-tagging origin-name gold]
  " filters to only objects that belong the given data group.
    this function will be radically simplified once :object-data-group
    finds a better home in the collection hierarchy "
  (let [filtered
     (filter
        (fn [object-tagging]
           (let
             [taggings-groups (val object-tagging)
              object-data-origin (:object-data-origin (get-single #(val=! % :tagging-group-name gold) taggings-groups))]

             (= object-data-origin origin-name)))

        objects-tagging)]

    filtered))


(defn ^:private evaluate [accuracy-model]

  (let  ;; get all the base data ready

    [data (read-data)

     gold (:gold-set data)
     classifiers-under-test (:result-sets data)

     gold-taggings (get-canonical-tagging data gold)

     classifiers-under-test-taggings
       (flatten
         (map (partial get-canonical-tagging data) classifiers-under-test))

     all-taggings
       (apply merge gold-taggings classifiers-under-test-taggings)

     target-tag-set
       (get-tag-set (flatten (map :taggings gold-taggings)))

     ; list of all objects for classification
     objects-tagging
       (group-by :object-id all-taggings)

     execution-config-base ;; this execution-config will remain invariant throughout execution combos
       { :objects-tagging objects-tagging
         :gold gold }]

     (println "gold tagging contains" (count target-tag-set) "unique tags:")
     (cprint target-tag-set)

     (let
        [classifiers-dim
           {:name :classifier
            :vals classifiers-under-test
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] (assoc evaluation-config :test-tagging-group-name current-dim-val))}

         tags-dim
           {:name :tag
            :vals target-tag-set
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] (assoc evaluation-config :test-tag current-dim-val))}

         at-n-dim
           {:name :n
            :vals [1 2 3]
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] (assoc evaluation-config :n current-dim-val))}

         in-out-domain
           {:name :in-domain-only?
            :vals [:yes :no]
            :evaluation-config-transform
            (fn [current-dim-val evaluation-config] ;; must base on current to maintain commutativity
               (let [current (:objects-tagging evaluation-config)]
                 (assoc
                   evaluation-config
                   :objects-tagging (case current-dim-val
                      :yes (filter-in-domain current gold)
                      :no  current))))}

         in-out-corpus
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
                      :all current))))}

         dimensions
           (vec
             [in-out-corpus
              in-out-domain
              tags-dim
              at-n-dim
              classifiers-dim])] ; evaluation dimensions

         (evaluate-all-dimensions accuracy-model dimensions execution-config-base))))



(defn execute []
    (let [evaluation (doall (evaluate get-accuracy-at))
          accuracy-model-name "accuracy-at"
          file-with-parents (partial file-with-parents (list "output" accuracy-model-name))]

      ;; writing the raw results to equivalent edn and json files
      (spit (file-with-parents "raw.edn") (with-out-str (pprint evaluation))) ; note! any downstream println will go to the file too
      (spit (file-with-parents "raw.json") (to-json evaluation {:pretty true :escape-non-ascii false}))
      (write-csv (file-with-parents "raw.csv") (csv-format evaluation))
      (println "outputs have been written to output directory")
      #_(do
        (println "launching swing output viewer..")
        (inspect/inspect-tree evaluation))))


