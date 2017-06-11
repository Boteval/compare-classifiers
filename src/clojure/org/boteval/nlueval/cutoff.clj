(ns org.boteval.nlueval.cutoff)

(defn confidence-cutoff-filter
  [taggings cutoff-value]

  " filters out all tags having a confidence lower than the supplied cutoff value "

  (filter
    (fn
      [tagging]
      (>= (:confidence tagging) cutoff-value))
    taggings)

)
