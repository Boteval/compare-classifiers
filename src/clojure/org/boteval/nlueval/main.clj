(ns org.boteval.nlueval.main
  (:require [org.boteval.nlueval.execute :refer :all]))

(defn -main []
  (let [ready-data (ready-data)]
    (execute
      (dims ready-data)
      ready-data)))

