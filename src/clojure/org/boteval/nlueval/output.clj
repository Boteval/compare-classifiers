(ns org.boteval.nlueval.output
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
))

(defn ^:private de-keyword [value]
  " turns any keyword to a regular string, to avoid presenting EDN keywords in user-facing CSV "
  (if
    (keyword? value)
    (name value)
    value))

(defn csv-format [evaluation]
  " turns the evaluation collection into csv writing input "
  (assert (apply = (map keys evaluation))
    "internal error: evaluation result data has inconsistent row keys, cannot be converted to csv")
  { :headers (map name (keys (first evaluation))) ;; as all rows have same key set
    :data
      (map
        #(map de-keyword %)
      (map vals evaluation)) })

(defn write-csv [path filename {:keys [headers data]}]
  (println headers)
  (println (first data))
  (with-open [out-file (io/writer filename)] ; TODO: make a writeer out of a clojure.java.io/file to include an OS agnostic path to write the CSV file under the output dir
    (csv/write-csv
      out-file
      (cons headers data))))
