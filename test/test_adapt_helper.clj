(ns test-adapt-helper
  (:use [adapt :only (adapt-protocol)]
        [test-adapt-helper2 :only (Foo2 bar2)]))

(defprotocol Foo
  (bar [_])
  (baz [_ _]))

(defn baz-delegate [foo x]
  (baz foo x))

(adapt-protocol Foo Foo2
                (bar2 [foo]
                      (str "bar2-" (bar foo)))
                (baz2 [_ _]
                      "baz2"))
