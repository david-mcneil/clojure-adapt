clojure-adapt
====

Allow Clojure protocols to be extended to other protocols. More details are 
[on my blog](http://david-mcneil.com/post/3495351254/clojure-protocol-adapters).

Usage
----

    (defprotocol Animal
      (speak [_]))

    (defprotocol Dog
      (bark [_]))

    (adapt-protocol Dog Animal
                    (speak [dog]
                           (bark dog)))

When a function from the Animal protocol is invoked on an object, x, which
does not satisfy the Animal protocol then if x satisfies the Dog
protocol the class of x will automatically be extended to support the
Animal protocol using the adapter methods provided in the
adapt-protocol call.

Notes
----

* A protocol can be extended to multiple protocols.
* If an object satisfies multiple protocols that have been adapted to a given
  protocol then it is indeterminate which adapter will be used.
* If adapt-protocol is invoked multiple times for the same pair of
  protocols then the new adapter will only be installed for future
  classes. That is, adapters that have been previously "installed" for
  a class will not be changed.

History
----

At the Clojure Conj conference in 2010 Rich Hickey described an
approach of dynamically extending a protocol to another protocol. This
is an implementation of that idea.

----

The use and distribution terms for this software are covered by the Eclipse 
Public License 1.0 http://opensource.org/licenses/eclipse-1.0.php.