(ns org.boteval.nlueval.input
  (:require
     [clojure.pprint :refer [pprint]]
     [puget.printer :refer [cprint]]
     [clojure.inspector :as inspect :refer [inspect-tree]]
     [clojure.data.csv :as csv]
     [clojure.java.io :as io]
     [clojure.set :refer :all]))


(defn ^:private get-result-sets-mapping [headers-mapping]
  " get all result sets according to the headers mapping file "

  (let [result-sets (:classification-result-sets headers-mapping)]
       (map (fn [[classifier-name tags]]
                        (hash-map
                           classifier-name
                           (map (fn [[tag confidence confidence-scale]]
                                  (hash-map
                                    :tag tag
                                    :confidence confidence
                                    :confidence-scale confidence-scale))
                                 tags)))
                       result-sets)))

(defn ^:private validate-and-fix-unique [data object-id-mapping]

  " validates the given dataset for object-id uniqueness and throws away identical duplicates if it is not that-way valid.
    returns the de-dupclicated dataset, or nil if no de-duplication was required.

    aborts in case any object-id has multiple non-identical rows,
    as that would mean we cannot deduce which duplicates to throw away and which one to keep "

  (let
    [ids (map object-id-mapping data)
     unique-ids (set ids)]

    (if (> (count ids) (count unique-ids))

       ;; try to de-duplicate
       (let
         [duplicates
            (filter
               #(> (count (val %)) 1)
               (group-by object-id-mapping data))

          duplicate-ids
            (map key duplicates)]

         (do
           (println
              (str
                "warning: the input data contains duplicate object ids ― the data has duplicate values for " (name object-id-mapping) "s: "
                (pr-str duplicate-ids) "\nde-duplicating..."))

           ;; abort if any id has non-identical rows
           (doall
             (map
               #(if
                  (not (apply = (val %))) ; are duplicates for the current id identical?
                    (do
                      (println (str
                         "warning: could not de-duplicate ― for the same id " (key %) ", some of the following data rows defer in content:\n\n"
                         (clojure.string/join "\n\n" (val %))))
                      (throw (Exception. "\n\n― given the above, this dataset is invalid and thusly rejected!" ))))
             duplicates))

           ;; return the original dataset, deduplicated.
           ;; row order is not guaranteed.
           (let
             [de-duplicated
              (map
                #(first (val %))
                (group-by object-id-mapping data))

              now-ids (map object-id-mapping de-duplicated)
              now-unique-ids (set now-ids)]

             (println
               (- (count ids) (count now-ids)) "rows/objects removed during de-duplication."
               "rows/objects after de-duplication:" (count now-ids))

             (assert (= (count unique-ids) (count now-unique-ids)) "internal error - deduplication failed")
             (assert (= (count now-unique-ids) (count now-ids)) "internal error - deduplication failed")
             (assert (= (set now-unique-ids) (set now-ids)) "internal error - deduplication failed")
             (assert (> (count ids) (count now-ids)) "internal error - deduplication failed")

             ;; return the de-duplicated dataset
             de-duplicated)))

       ;; else signal that no data recovery was necessary
       nil)))



(defn read-data []

  " loads the input data and its mapping "

  (let [relative-path "input" file-name "mapping.edn"
        input-mapping (read-string (slurp (io/file relative-path file-name)))]

    (println (str "input mappings loaded from " file-name " under the " relative-path " directory"))

    (let
      [csvs
        (map
          (fn
            [data-file]
              (with-open [input-file (io/reader (io/file "input" (:file data-file)))]
                (println "reading data file" (:file data-file))
                { :data-group (:data-group data-file)
                  :content (doall (csv/read-csv input-file)) }))
          (:data-files input-mapping))

       headers
         (do
           (assert (apply = (map #(first (:content %)) csvs))
             " to use multiple data files, all data files must have identical column headers ")
           (map keyword (first (:content (first csvs))))) ; given they all have the same headers row...

       data
         (flatten (map
           (fn [csv]
             (map
               (fn [content-row]
                 (zipmap (cons :data-group headers) (cons (:data-group csv) content-row)))
               (rest (:content csv))))
          csvs))

       object-id-mapping (keyword (:object-id (:headers-mapping input-mapping)))
       tagging-set-names (set (map key (:classification-result-sets (:headers-mapping input-mapping))))
       gold :gold ; the result set having the keyword :gold is assumed to be the gold result set
       result-set-names (difference tagging-set-names (set [gold]))

       data (or (validate-and-fix-unique data object-id-mapping) data) ; if needed, de-duplicate the data

       tagging-group-mappings (get-result-sets-mapping (:headers-mapping input-mapping))]

         (println "the following tagging sets are described by the mapping file:" (clojure.string/join ", " (map name result-set-names)))
         (println "tagging set" gold "taken as the gold dataset")
         (println "input data comprises" (count data) "objects")
         (println "using object id header:" (name object-id-mapping))

         { :data data
           :object-id-mapping object-id-mapping
           :gold-set gold
           :result-sets result-set-names
           :tagging-group-mappings tagging-group-mappings })))
