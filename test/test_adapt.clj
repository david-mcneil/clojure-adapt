(ns test-adapt
  (:require [test-adapt-helper])
  (:use [clojure test]
        [adapt :only (adapt-protocol* adapt-protocol get-function-symbols emit-impl lookup-f)]
        [clojure.pprint :only (pprint)]
        [test-adapt-helper :only (Foo bar baz-delegate)]
        [test-adapt-helper2 :only (bar2)])
  (:import [java.util Date]))

(defn ns-fixture
  "Test harness that binds the namespace so it will run under lein"
  [f]
  (binding [*ns* (find-ns 'test-adapt)]
    (f)))

(use-fixtures :each ns-fixture)

(defprotocol Animal
  (speak [_]))

(defprotocol Dog
  (bark [_]))

(defprotocol Cat
  (talk [_]))

(extend-protocol Dog String
                 (bark [s] (str "arf " s)))

(extend-protocol Cat java.util.Date
                 (talk [d] (str "meow " (.getTime d))))

(deftest test-get-function-symbols
  (is (= ['speak]
           (get-function-symbols Animal))))

(deftest test-emit-impl
  (is (= {:foo '(fn [a] (+ 1 a))}
         (emit-impl '((foo [a] (+ 1 a))))))

  (is (= {:foo '(fn [a] (+ 1 a))
          :bar '(fn [x y z] nil)}
         (emit-impl '((foo [a] (+ 1 a))
                      (bar [x y z] nil)))))

  (is (= {:foo '(fn
                  ([a]
                     (+ 1 a))
                  ([a b]
                     (+ b a)))}
         (emit-impl '((foo ([a]
                              (+ 1 a))
                           ([a b]
                              (+ b a))))))))

(deftest test-lookup-f
  (is (= "arf hi"
         ((lookup-f Dog 'bark) "hi"))))

(deftest test-adapt-protocol
  (adapt-protocol Dog Animal
                  (speak [dog]
                         (bark dog)))
  (is (= "arf hello"
         (speak "hello"))))

(deftest test-adapt-protocol*
  (adapt-protocol* Cat Animal
                   {:speak (fn [cat]
                             (talk cat))})
  (is (= "meow 100"
         (speak (Date. (long 100))))))

;;;;

(defprotocol Animal2
  (speak2 [_]))

(defprotocol Dog2
  (bark2 [_]))

(defprotocol Cat2
  (talk2 [_]))

(extend-protocol Dog2 String
                 (bark2 [s] (str "arf " s)))

(extend-protocol Cat2 java.util.Date
                 (talk2 [d] (str "meow " (.getTime d))))

(deftest test-adapt-protocol-twice-fails
  (adapt-protocol Dog2 Animal2
                  (speak2 [dog]
                          (bark2 dog)))
  (adapt-protocol Cat2 Animal2
                  (speak2 [cat]
                          (talk2 cat)))
  (is (= "meow 100"
         (speak2 (Date. (long 100)))))

  (is (= "arf hello"
         (speak2 "hello"))))

;;

(defprotocol Animal3
  (speak3 [_]))

(defprotocol Dog3
  (bark3 [_]))

(defprotocol Cat3
  (talk3 [_]))

(extend-protocol Dog3 String
                 (bark3 [s] (str "arf " s)))

(extend-protocol Cat3 java.util.Date
                 (talk3 [d] (str "meow " (.getTime d))))

(deftest test-adapt-protocol-twice-succeeds
  ;; adapt a protocol, exercise it, then adapt another protocol
  (adapt-protocol Dog3 Animal3
                  (speak3 [dog]
                          (bark3 dog)))
  (is (= "arf hello"
         (speak3 "hello")))

  (adapt-protocol Cat3 Animal3
                  (speak3 [cat]
                          (talk3 cat)))
  (is (= "meow 100"
         (speak3 (Date. (long 100))))))

;;

(defprotocol Animal4
  (speak4 [_ n])
  (perform4 [_])
  (stay4 [_]))

(defprotocol Dog4
  (bark4 [_ n])
  (sit4 [_]))

(extend-protocol Dog4 String
                 (bark4 [s n] (apply str (repeat n (str "arf ")))))

(deftest test-adapt-protocol-many-functions
  (adapt-protocol Dog4 Animal4
                  (speak4 [dog n]
                          (bark4 dog n))
                  (stay4 [dog]
                         "staying"))
  (is (= "arf arf arf "
         (speak4 "hello" 3)))

  (is (= "staying"
         (stay4 "hello"))))

(deftest test-adapt-protocol-failure
  (is (thrown-with-msg? RuntimeException #"No implementation of protocol: test-adapt/Animal4 found for class: class java.util.Date"
        (speak4 (Date. (long 100)) 4))))

;;

(defprotocol FooLocal
  (blah [_]))

(extend-protocol FooLocal String
                 (blah [_] "blah"))

(extend-protocol Foo java.util.Date
                 (bar [date]
                      (str (.getTime date)))
                 (baz [date _]
                      (str "baz-" (.getTime date))))

(deftest test-adapt-protocol-from-other-namespace
  (adapt-protocol FooLocal Foo
                  (bar [_]
                       "bar")
                  (baz [_ _]
                       "baz"))
  (is (= "bar"
         (bar "test")))
  
  (is (= "bar2-100"
         (bar2 (Date. (long 100)))))

  (is (= "baz-100"
         (baz-delegate (Date. (long 100)) nil))))

;; (run-tests)
