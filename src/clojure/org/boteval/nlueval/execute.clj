(ns org.boteval.nlueval.execute
  (:require
    [clojure.pprint :refer [pprint]]
    [puget.printer :refer [cprint]]
    [clojure.inspector :as inspect :refer [inspect-tree]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
    [org.boteval.nlueval.input :refer :all]
    [org.boteval.nlueval.canonical :refer :all]
    [org.boteval.nlueval.dimensional-evaluation :refer :all]))


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

     ; group by tagging's object id, and add indication for in/out domain,
     ; yielding a list of objects with their meta-data and taggings
     objects-tagging
       (group-by :object-id all-taggings)

     execution-config-base ;; this execution-config will remain invariant throughout execution combos
       { :objects-tagging objects-tagging
         :gold gold
         :n 3 }]

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

         dimensions (vec [classifiers-dim tags-dim at-n-dim])] ; evaluation dimensions

         (evaluate-all-dimensions dimensions execution-config-base))))



(defn execute []
    (let [evaluation (evaluate)]

      ;; writing the raw results to equivalent edn and json files
      (do
         (spit (io/file "output" "raw.edn") (with-out-str (pprint evaluation))) ; note! any downstream println will go to the file too
         (spit (io/file "output" "raw.json") (to-json evaluation {:pretty true :escape-non-ascii false}))
         (println "outputs have been written to output directory")
         (println "launching swing output viewer..")
         (inspect/inspect-tree evaluation))))


