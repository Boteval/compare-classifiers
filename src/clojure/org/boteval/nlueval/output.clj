(ns org.boteval.nlueval.output
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
    [org.boteval.nlueval.util :refer :all]
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
    "internal error: data has inconsistent row keys, cannot be converted to csv")
  { :headers (map name (keys (first evaluation))) ;; as all rows have same key set
    :data
      (map
        #(map de-keyword %)
      (map vals evaluation)) })

(defn write-csv [path {:keys [headers data]}]
  {:pre [(instance? java.io.File path)]}

  #_(do
    (println headers)
    (println (first data)))

  (with-open [out-file (io/writer path)] ; TODO: make a writeer out of a clojure.java.io/file to include an OS agnostic path to write the CSV file under the output dir
    (csv/write-csv
      out-file
      (cons headers data))))


(defn trace-write
  [evaluation-name args objects-analysis]
  {:pre [(map? args)]}
  " outputs a per-object analysis in csv format. for use for tracing
    the per-object analysis of a single multi-dimensional evaluation.
    the file name will indicate the args of the evaluation "
  (let
    [path (list "output" evaluation-name "traces")
     filename (str args ".csv")
     file-with-parents (file-with-parents path filename)]

    (write-csv file-with-parents (csv-format objects-analysis))))


(defn write-evaluation-result [evaluation-name evaluation]
  " outputs an evaluation result "
  (let
    [path (list "output" evaluation-name)
     file-with-parents (partial file-with-parents path)]

    ;; writing the raw results to equivalent edn and json files
    (spit (file-with-parents "out.edn") (with-out-str (pprint evaluation))) ; note! any downstream println will go to the file too
    (spit (file-with-parents "out.json") (to-json evaluation {:pretty true :escape-non-ascii false}))

    ;; writing the csv results
    (write-csv (file-with-parents "out.csv") (csv-format evaluation))
    (println "outputs have been written to output directory" (.getPath (apply io/file path)))

    #_(do
    (println "launching swing output viewer..")
    (inspect/inspect-tree evaluation))))

