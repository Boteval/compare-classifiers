(ns nlueval.last-good-compare

  " tests whether current output is identical to a previously stashed output directory dubbed 'last good'.
    if no previously stashed output is found ― does nothing and only emits a message to stdout. "

  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [puget.printer :refer [cprint]]
            [clojure.inspector :as inspect :refer [inspect-tree]]
            [org.boteval.nlueval.util :refer :all]
            [org.boteval.nlueval.input.ready :refer :all]
            [org.boteval.nlueval.execute :refer :all]))

(deftest same-as-last-good

  (let [targets
          [ {:name "accuracy-at"
             :last-good-path-seq '("output" "last-good" "accuracy-at" "out.edn")
             :current-path-seq   '("output" "accuracy-at" "out.edn")}

            {:name "Godbole-accuracy"
             :last-good-path-seq '("output" "last-good" "Godbole-accuracy" "out.edn")
             :current-path-seq   '("output" "Godbole-accuracy" "out.edn")}]]

    (let [last-goods (map get-file-object (map :last-good-path-seq targets))]

      (if (every? #(.exists %) last-goods)
        (time (do
          (let [ready-data (ready-data :data-files)]
            (execute
              (dims ready-data)
              ready-data))

          (doall
            (map
              (fn comparer [{:keys [name current-path-seq last-good-path-seq]}]
                (let
                  [current-file   (get-file-object current-path-seq)
                   last-good-file (get-file-object last-good-path-seq)
                   current        (slurp current-file)
                   last-good      (slurp last-good-file)]

                  (binding [*print-length* 0] ; because the default output of `is` is ill-suited, user should refer to the named files and/or diff them with a diff tool
                    (is (= current last-good)
                        (str "current output data is different than last-good for " name ",\n"
                             "compared output data files: " (.getPath current-file) ", " (.getPath last-good-file) "\n"
                             "in case you assumed no change to the output from prior runs, this test refutes your assumption,\n"
                             "and otherwise it might be a good time to pin down a last good output.")))))

              targets))))

      (println "info: to test for changes from the 'last good' output data, a last good output can be placed inside 'output/last-good', by simply copying the contents of the output directory there.")))))
