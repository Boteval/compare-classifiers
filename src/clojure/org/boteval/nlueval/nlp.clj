(ns org.boteval.nlueval.nlp
  " faint glimpses of nlp that shouldn't really belong here "
  (:require
    [clojure.test :refer :all]))

(with-test
  (defn count-sentences [text]

    " count sentences in Hebrew text.
      assumes no foreign dot initials.
      arbitrarily avoids the ambiguity of pathogical cases of the general form a.5b"

    (let
      [number-cleaned (clojure.string/replace text #"[0-9]*\.[0-9]+" "foo")
       dot-sequences-cleaned (clojure.string/replace number-cleaned #"[\.!?\n]+" ".")]
      (count
        (filter not-empty
          (clojure.string/split dot-sequences-cleaned #"\.")))))

  (is (= (count-sentences "aaa.... b.. c. d") 4))
  (is (= (count-sentences "aaa.... b.. c. d.") 4))
  (is (= (count-sentences "aaa?!?? b.. c. d") 4))
  (is (= (count-sentences "aaa?!??.. b.. c? d!") 4))
  (is (= (count-sentences "aaa") 1))
  (is (= (count-sentences "aaa? b c. d") 3))
  (is (= (count-sentences "aaa..") 1))
  (is (= (count-sentences "") 0))

  (is (= (count-sentences "aaa 0.5 bbb") 1))
  (is (= (count-sentences "aaa 0.5% bbb") 1))
  (is (= (count-sentences "aaa .5 bbb") 1))
  (is (= (count-sentences "aaa 19.5 bbb") 1))
  (is (= (count-sentences "aaa 19.5. bbb") 2))
  (is (= (count-sentences "aaa 19.5. bbb") 2))

)



