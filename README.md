# nlu-eval

A tool for comparative evaluation of categorical classification outputs.

## Usage

Git clone this repo.

1. [Install leiningen](https://leiningen.org/#install)

2. Place the following into the `input` directory:

        2.1 the CSV data file containing the tagging results to be analyzed

        2.2 a mapping file from possibly proprietary header names used in your CSV → to names used by this program. see sample mapping file below.

3. from a terminal session, run: `lein run`.

Note that outputs will be generated under the directory "output".


## Sample Mapping file

The following sample demonstrates how the required mapping file informs the program as to the semantics of the input csv.
Your real mapping file should be placed under directory "input", and it must be called `mapping.edn`.

```clojure
;;;
;;; this mapping file advises the program where to read the data from, and how to read it
;;;

{
  ; name of the provided input data file, expected under directory "input"
  :data-file file1.csv

  ; mapping of column header names, to enable reading the gold and
  ; result classifications, from the above provided input data file
  :headers-mapping

    { :object-id "id"
      :object "msg"

      ; every classification header set is a tuple comprising: a header name,
      ; a header name for its score ― or a value in case no score column is provided,
      ; and a maximum score value (the latter will be used as a normalization factor).

      :classification-result-sets

        {
          ; gold tags
          :gold
           [[:label1 1 1]
            [:label2 1 1]]

          ; classifier foo's tagging
          :foo
           [[:foo-label1 :foo-label1-score 100]
            [:foo-label2 :foo-label2-score 100]]

          ; classifier bar tagging
          :bar
           [[:bar-label1 :bar-label1-score 1]
            [:bar-label2 :bar-label2-score 1]]}}}
```


## License

Distributed under the Eclipse Public License either version 1.0
