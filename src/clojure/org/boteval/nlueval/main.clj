(ns org.boteval.nlueval.main
  (:require
    [org.boteval.nlueval.input.ready :refer :all]
    [org.boteval.nlueval.execute :refer :all]))

(defn -main []
  (let [ready-data (ready-data :data-files)]
    (execute
      (dims ready-data)
      ready-data)))

