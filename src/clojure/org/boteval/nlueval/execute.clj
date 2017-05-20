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
    [org.boteval.nlueval.evaluate :refer :all]))

(defn execute []
  (let
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
       (group-by :object-id all-taggings)]

    (let
      [evaluate
        (fn
          [object-taggings gold classifier-under-test]
          {:pre [(keyword? gold) (keyword? classifier-under-test)]}

          " gets the accuracy evaluation per tag for the given classifier "

          (map
            (fn [target-tag]
              (hash-map
                :classifier classifier-under-test
                :tag target-tag
                :accuracy (get-accuracy-at objects-tagging gold classifier-under-test target-tag 3)))
            target-tag-set))

       ; evaluate for all classifiers under test
       evaluation
         (flatten (map
           (partial evaluate objects-tagging gold)
           classifiers-under-test))]

      (println "gold tagging has" (count target-tag-set) "unique gold tags:")
      (cprint target-tag-set)

      ;; writing the raw results to equivalent edn and json files
      (do
         (spit (io/file "output" "raw.edn") (with-out-str (pprint evaluation))) ; note! any downstream println will go to the file too
         (spit (io/file "output" "raw.json") (to-json evaluation {:pretty true :escape-non-ascii false}))
         (println "outputs have been written to output directory")
         (println "launching swing output viewer..")
         (inspect/inspect-tree evaluation)))))


