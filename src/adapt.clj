(ns adapt
  "Adapt a protocol to support another protocol.")

(defonce protocol-adapters (ref {}))

;; helper functions

(defn dynamic-extend
  "Use the global protocol-adapters map to find the adapter to use. protocol-adapters is a nested map keyed first on the protocol that the adapter implements, then keyed on the protocol being adapted, finally it contains the adapter map to use. For a given x, walk through all of the protocols and find one that x satisfies, then use the corresponding adapter."
  [protocol-symbol x]
  (let [adapter-map (get @protocol-adapters protocol-symbol)
        satisfied (first (filter #(satisfies? @(resolve %) x) (keys adapter-map)))
        adapter (get adapter-map satisfied)]
    (if adapter
      (do (extend (class x) @(resolve protocol-symbol)
                  adapter)
          x)
      (constantly (throw (IllegalArgumentException. (str "No implementation of protocol: "
                                                         protocol-symbol
                                                         " found for class: " (class x))))))))

(defn get-protocol-symbol [protocol]
  (symbol (str (.ns (:var protocol))
               "/"
               (.sym (:var protocol)))))

(defn lookup-f
  "Find a function object given its symbol."
  [other-protocol sym]
  @(ns-resolve (symbol (str (.ns (:var other-protocol)))) sym))

(defn adapt
  "Create a function that invokes an adapter-f on the first arg before trying to invoke the function defined by f-symbol. The function needs to be dynamically resolved because calling the adapter-f can change what function is bound to the symbol."
  [other-protocol f-symbol adapter-f]
  (fn [x & args]
    (apply (lookup-f other-protocol f-symbol) (conj args (adapter-f x)))))

(defn get-function-symbols
  "Return a sequence of symbols which are the names of the functions in the protocol."
  [protocol]
  (map (comp symbol name) (vals (:method-map protocol))))

(defn emit-impl
  "Based on clojure.core.emit-impl in core_deftype.clj. Convert '(foo [a] (+ 1 a)) to {:foo (fn [a] (+ 1 a))}"
  [fs]
  (zipmap (map #(-> % first keyword) fs)
          (map #(cons 'fn (drop 1 %)) fs)))

(defn create-function-map
  "Produce a function map for use with the 'extend' function that implements the functions of other-protocol."
  [protocol other-protocol adapter]
  (let [adapter-f #(dynamic-extend (get-protocol-symbol other-protocol) %)
        function-symbols (get-function-symbols other-protocol)]
    (dosync
     (alter protocol-adapters assoc-in [(get-protocol-symbol other-protocol)
                                        (get-protocol-symbol protocol)] adapter))
    (zipmap (map keyword function-symbols)
            (map #(adapt other-protocol % adapter-f) function-symbols))))

;; public entry points

(defn adapt-protocol*
  "Extend other-protocol to protocol using the method implementations in the adapter map."
  [protocol other-protocol adapter]
  (extend Object other-protocol
          (create-function-map protocol other-protocol adapter)))

(defmacro adapt-protocol
  "Wrapper around adapt-protocol* that provides a more natural syntax for the impls."
  [protocol other-protocol & impls]
  `(adapt-protocol* ~protocol ~other-protocol ~(emit-impl impls)))
