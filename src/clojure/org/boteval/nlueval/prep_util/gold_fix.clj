(ns org.boteval.nlueval.prep-util.gold-fix
  (:require
     [clojure.pprint :refer [pprint]]
     [puget.printer :refer [cprint]]
     [clojure.inspector :as inspect :refer [inspect-tree]]
     [clojure.data.csv :as csv]
     [clojure.java.io :as io]
     [clojure.set :refer :all]
     [clojure.data.csv :as csv]
     [clojure.java.io :as io]
     [org.boteval.nlueval.util :refer :all]
     [org.boteval.nlueval.output :refer :all])
  (:gen-class))


(defn ^:private merge-join [id-keyword overriding m]
  {:pre [(keyword? id-keyword)
         (map? m)]}

  " merges the map from the overriding collection which matches the id of the given map, into the given map "

  (merge
    m
    (get-single #(= (id-keyword %) (id-keyword m)) overriding)))


(defn -main [gold-file-name input-file-name output-file-name]
  {:pre
   [(string? gold-file-name)
    (string? input-file-name)
    (string? output-file-name)]}

  " main for updating the gold taggings of provided input-files "

  (println "substituting gold tags...")

  (let
    [base-path "input"
     input (read-csv base-path input-file-name)
     gold (read-csv base-path gold-file-name)
     gold-data-cleansed (map #(select-keys % [:intent1 :intent2 :intent3]) (:data gold))
     gold-data-cleansed-sorted (sort-by :id (:data gold))

     updated-data
        (map
          (partial merge-join :id gold-data-cleansed-sorted)
          (:data input))

     output-path (list base-path "pre-processed")
     output-file (file-with-parents output-path output-file-name)]

     (do
       (cprint (first updated-data))
       (cprint (:headers input))
       (cprint (order-by-headers (:headers input) (first updated-data))))

     (write-csv
        output-file
        { :headers
            (map name (:headers input))
          :data
            (map
               #(order-by-headers (:headers input) %)
               updated-data) })

     (println "output written to" (.getPath output-file))))


