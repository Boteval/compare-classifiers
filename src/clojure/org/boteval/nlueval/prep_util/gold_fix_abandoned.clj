#_(ns org.boteval.nlueval.prep-util.gold-fix-abandoned
  (:require
     [clojure.pprint :refer [pprint]]
     [puget.printer :refer [cprint]]
     [clojure.inspector :as inspect :refer [inspect-tree]]
     [clojure.data.csv :as csv]
     [clojure.java.io :as io]
     [clojure.set :refer :all]
     [clojure.data.csv :as csv]
     [clojure.java.io :as io]
     [org.boteval.nlueval.input.ready :refer :all]
     [org.boteval.nlueval.output :refer :all])
  (:gen-class))


#_(defn ^:private override-gold-tagging [[id tagging-wrappers-seq] gold-tagging-wrapper]

  " returns the input map entry, with its gold tagging wrappar replaced.
    can turn it into a util function that replaces a sequence element that matches a predicate "

  [id
   (cons
     gold-tagging
     (remove #(= (:tagging-group-name tagging-wrappers-seq) :gold) tagging-wrappers-seq))])


#_(defn ^:private update-with-gold [input-data gold-override-data]

  " updates the internal data structure with the supplied gold override data ―
    for each id in the input data, replaces the gold data with the one having
    the same id in the gold override data, and thus assumes all ids exist
    in the gold data override data supplied "

  (let
     [gold-tagging-override (into (sorted-map) (:objects-tagging gold-only-data))]

      #_(cprint (take 10 input-data))
      (update
        input-data
        objects-tagging
        (fn replace-gold-tagging
           [objects-tagging]
           (map ; replaces the gold-tagging for each object, with the one from the override collection
             (fn
                [object-tagging]
                (override-gold-tagging object-tagging (get! gold-tagging-override (key object-tagging)))
             objects-tagging))))))



#_(defn -main []
  " main for updating the gold taggings of an input file "
  (let
    [updated
       (update-with-gold
        (ready-data :data-files)
        (ready-data :gold-tagging-files))]

     (write-csv
        (io/file "gold-substituted.csv")
        {:headers nil
         :data
           (:objects-tagging updated)})))






#_(inspect/inspect-tree input-data)
#_(inspect/inspect-tree gold-tagging-override)
