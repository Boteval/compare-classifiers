# compare-classifiers

A library for juxtaposing classification performance metrics of either multiple classifiers, or different versions of the same classifier, over the same dataset.

This library simply computes performance metrics of your choice, over an input CSV file containing gold and classifier predicted labels, while sporting the following humble features:

+ __multi-dimensional evaluation resulting in a data cube__  
It is easy to specify a plurality of dimensions, each ranging over its own set of values, and get the performance metric calculated for each combination of every value of each those dimensions ― in a single run.

+ __simple map-reduce specification of a performance metrics__  
To compute a performance metric over the input data, all it takes is writing or supplying a specification comprising two parts: a mapper which operates on every object, and a reducer that aggregates the computation for all objects of the dataset.
+ __seamless audit trail__  
Remember, we can easily get a cube where each metric is computed per combinatoric combination of all values of all dimensions. Each evaluation seamlessly gets its own audit file, where the mapper's result per object is neatly recorded ― so you can always trace/audit/prove how your calculation was made.

+ __convenient CSV input format__  
As input, this library expects a CSV file containing object-ids and their gold tagging, along taggings returned by one or more classifiers. If the objects are text to be categorized, you may include the actual texts alongside the object-id column ― the library will ignore all columns that are not specifically designated through a mapping file.  

  Rather than relying on a hardwired naming convention for CSV column headers, this library can process any CSV file, as long as the first row comprises the column headers ― an input mapping file is used to tell the library which columns to look at (more on the mapping file syntax below).

+ __concurrency__
While not yet performance optimized, this library will squeeze out all the CPU power available on your machine, parallelizing its computation for different dimensions.

+ __CSV output cube__  
It can be very convenient exploring your metrics across your dimensions, by then playing with the output data cube in a spreadsheet tool like google spreadsheets or Microsoft Excel.

## Usage

1. Add the following dependency in project.clj:

  ```clojure
  [org.boteval.nlueval "0.0.1"]
  ```

2. Place the following under an `input` directory in your project:

        2.1 your one or more CSV data files containing a gold tagging alongside a classifier's tagging per object.

        2.2 a mapping file from header names used in your CSV → to names used by this program. see the sample mapping file copied here below.

3. Follow the example code in the example usage project at https://github.com/Boteval/compare-classifiers-example for defining your performance metrics and any dimensions you wish to iterate them on.

Note that outputs will be generated under the directory "output".

## Sample Mapping file

The following sample demonstrates how the required mapping file informs the program as to the semantics of the input csv files, as well as additional configuration semantics. Your real mapping file should be placed under directory "input", and it must be called `mapping.edn`.

```clojure
;;;
;;; this mapping file advises the program where to read the data from, and how to read it
;;;

{
  :data-files
    ; one or more input data files, expected under directory "input".
    ; to maintain traceability, the data within each file will be associated with the name provided by :data-group.
    [ {:file "input-file-1.csv"
       :data-group :corpus}

      {:file "input-file-2.csv"
       :data-group :exa-corpus} ]

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

  ;; sometimes input data is dirty, containing irrelevant classes.
  ;; if the following entry is included, classes outside this list will be ignored.
  :valid-classes
    ["class A"
     "class B"
     "class C"]
}
```

## Important notes about input data conventions

1. All input data files must have the same headers structure.
2. An object id must be unique across all input data files provided to a single run.


## License

Distributed under the Eclipse Public License either version 1.0
