(ns nlueval.last-good-compare

  " tests whether current output is identical to previously stashed output directory.
    useful when validating through a specific dataset, which should not be part of the repo. "

  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [org.boteval.nlueval.execute :refer :all]))

(deftest same-as-last-good

  (let [last-good (io/file "output" "last-good" "accuracy-at" "out.edn")]

    (if (.exists last-good)
      (do
        (let [ready-data (ready-data)]
          (execute
            (dims ready-data)
            ready-data))

        (let
          [current (slurp (io/file "output" "accuracy-at" "out.edn"))
           last-good (slurp last-good)]
          (binding [*print-length* 1]
            (is (= current last-good) "current outupt is different than last-good"))))

      (println "info: to test against a 'last good' output, a copy of 'output' is used from 'output/last-good'"))))


