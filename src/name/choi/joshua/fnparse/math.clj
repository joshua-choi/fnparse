(ns name.choi.joshua.fnparse.math
  (:require [name.choi.joshua.fnparse.cat :as r]
            [clojure.template :as template]
            name.choi.joshua.fnparse.cat.test)
  (:use [clojure.test :only #{deftest is run-tests}]))

(set! *warn-on-reflection* true)

(declare expr)

(def digit
  (r/hook #(Integer/parseInt (str %))
    (r/term "a decimal digit" #(Character/isDigit (char %)))))

(template/do-template [rule-name token]
  (def rule-name (r/lit token))
  plus-sign \+, minus-sign \-, multiplication-sign \*, division-sign \/,
  opening-parenthesis \(, closing-parenthesis \))

(def indicator
  (r/label "an indicator"
    (r/+ plus-sign minus-sign multiplication-sign division-sign
         opening-parenthesis closing-parenthesis)))

(def number-expr
  (r/label "a number"
    (r/+ (r/for [first-digits #'number-expr, next-digit digit]
           (r/+ (* 10 first-digits) next-digit))
         digit)))

(def symbol-char (r/except "a symbol character" r/anything_ indicator))

(def symbol-content
  (r/+ (r/for [first-char symbol-char, next-chars #'symbol-content]
         (cons first-char next-chars))
       (r/hook list symbol-char)))

(def symbol-expr
  (r/label "a symbol" (r/hook #(apply str %) symbol-content)))

(def terminal-level-expr
  (r/+ number-expr symbol-expr))

(def parenthesized-expr
  (r/circumfix-conc opening-parenthesis #'expr closing-parenthesis))

(def function-expr (r/vcat symbol-expr parenthesized-expr))

(def parenthesized-level-expr
  (r/+ parenthesized-expr terminal-level-expr))

(def function-level-expr
  (r/+ function-expr parenthesized-level-expr))

(def pos-neg-level-expr
  (r/+ (r/vcat (r/+ plus-sign minus-sign) function-level-expr)
       function-level-expr))

(def multiplication-level-expr
  (r/+ (r/vcat
         #'multiplication-level-expr
         (r/+ multiplication-sign division-sign)
         pos-neg-level-expr)
       pos-neg-level-expr))

(def addition-level-expr
  (r/+ (r/vcat
         #'addition-level-expr
         (r/+ plus-sign minus-sign)
         multiplication-level-expr)
       multiplication-level-expr))

(def expr addition-level-expr)

(deftest various-exprs
  (is (match? expr "3+1*cos(-(-5)+sin(2))"
        :product? #(= % [3 \+ [1 \* ["cos" [[\- [\- 5]] \+ ["sin" 2]]]]])))
  (is (non-match? expr "*3+1*cos(-(-5)+sin(2))"
        :labels #{"a number" "a symbol" "'-'" "'+'" "'('"}
        :position 0)))

(run-tests)