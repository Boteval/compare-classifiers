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
    [org.boteval.nlueval.canonical :refer :all]
    [org.boteval.nlueval.dimensional-evaluation :refer :all]))


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

    (println "out of" (count objects-tagging) "objects," (count filtered) "are considered in-domain")
    filtered))


(defn ^:private evaluate []

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

     ; subset of objects which are within-domain
     in-domain-only-objects-tagging
       (filter-in-domain objects-tagging gold)

     execution-config-base ;; this execution-config will remain invariant throughout execution combos
       { :objects-tagging objects-tagging
         :gold gold }]

     (println "gold tagging has" (count target-tag-set) "unique gold tags:")
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
            (fn [current-dim-val evaluation-config]
               (assoc
                 evaluation-config
                 :objects-tagging (case current-dim-val
                    :yes in-domain-only-objects-tagging
                    :no  objects-tagging)))}

         dimensions (vec [in-out-domain classifiers-dim tags-dim at-n-dim])] ; evaluation dimensions

         (evaluate-all-dimensions dimensions execution-config-base))))



(defn execute []
    (let [evaluation (doall (evaluate))]

      ;; writing the raw results to equivalent edn and json files
      (do
         (spit (io/file "output" "raw.edn") (with-out-str (pprint evaluation))) ; note! any downstream println will go to the file too
         (spit (io/file "output" "raw.json") (to-json evaluation {:pretty true :escape-non-ascii false}))
         (println "outputs have been written to output directory")
         (println "launching swing output viewer..")
         (inspect/inspect-tree evaluation))))


