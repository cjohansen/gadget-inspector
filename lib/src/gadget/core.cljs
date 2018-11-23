(ns gadget.core
  (:require [clojure.string :as str]))

(defmulti render-data identity)

(defonce store (atom {:atoms []}))

(def path-names
  {:gadget/JWT "JWT"})

(defn- prepare-path [path-elems]
  (loop [[x & xs] path-elems
         path []
         res []]
    (if (seq xs)
      (let [path (conj path x)]
        (recur (vec xs) path (conj res {:text (or (path-names x) (str x))
                                        :actions [[:set-path (first path) (rest path)]]})))
      (conj res {:text (str x)}))))

(defn- vtype [v]
  (cond
    (string? v) :string
    (keyword? v) :keyword
    (number? v) :number
    (boolean? v) :boolean
    (map? v) :map
    (vector? v) :vector
    (list? v) :list
    (nil? v) :nil
    (set? v) :set
    (symbol? v) :symbol
    (seq? v) :seq
    :default :object))

(defn- prep-key [k]
  {:val (cond
          (seq? k) (let [selection (take 11 k)]
                     (if (= (count selection) 11)
                       (str "(" (str/join " " (take 10 k)) " ...)")
                       (pr-str selection)))
          :default (pr-str k))
   :type (vtype k)})

(defn- constructor [v]
  (second (re-find #"function (.*)\(" (str (type v)))))

(def inline-length-limit 120)

(defn- too-long-for-inline? [v]
  (< inline-length-limit
     (.-length (pr-str v))))

(defmulti prep-val (fn [label path v] (vtype v)))

(defmethod prep-val :default [label path v]
  {:val (pr-str v) :type (vtype v)})

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(defmethod prep-val :string [label path s]
  (cond
    (re-find re-jwt s)
    {:val (pr-str (str (first (str/split s #"\.")) "..."))
     :type :jwt
     :actions [[:set-path label (conj path :gadget/JWT)]]}

    :default {:type :string :val (pr-str s)}))

(defn- inflect [n w]
  (if (= n 1)
    w
    (str w "s")))

(defn- summarize [pre c post & [w]]
  (let [num (count c)
        types (into #{} (map vtype c))
        w (or w (if (= 1 (count types)) (name (first types)) "item"))]
    (str pre num " " (inflect num w) post)))

(defn type-pref [v]
  (cond
    (keyword? v) 0
    (symbol? v) 1
    (string? v) 2
    (number? v) 3
    (map? v) 4
    (vector? v) 5
    (list? v) 6
    (set? v) 7
    (seq? v) 8
    (boolean? v) 9
    :default 10))

(defn sort-keys [m]
  (->> m
       (sort-by (comp pr-str first))
       (sort-by (comp type-pref first))))

(defn sort-vals [xs]
  (->> xs
       (sort-by pr-str)
       (sort-by type-pref)))

(defmethod prep-val :object [label path o]
  (let [constructor (constructor o)]
    {:val (str "object[" constructor "]")
     :type :object
     :constructor constructor}))

(defmethod prep-val :map [label path m]
  (let [prep-val (partial prep-val label path)]
    (if (too-long-for-inline? m)
      (let [ks (map first (sort-keys m))]
        (assoc
         (if (too-long-for-inline? ks)
           {:type :summary :val (summarize "{" ks "}" "key")}
           {:type :map-keys :val (map prep-val ks)})
         :actions [[:set-path label path]]))
      {:type :map
       :val (map (fn [[k v]] [(prep-key k) (prep-val v)]) (sort-keys m))})))

(defmethod prep-val :set [label path s]
  (if (too-long-for-inline? s)
    {:type :summary :val (summarize "#{" s "}") :actions [[:set-path label path]]}
    {:type :set :val (into #{} (map #(prep-val label path %) (sort-vals s)))}))

(defmethod prep-val :vector [label path v]
  (if (too-long-for-inline? v)
    {:type :summary :val (summarize "[" v "]") :actions [[:set-path label path]]}
    {:type :vector :val (map #(prep-val label path %) v)}))

(defmethod prep-val :list [label path l]
  (if (too-long-for-inline? l)
    {:type :summary :val (summarize "(" l ")") :actions [[:set-path label path]]}
    {:type :list :val (map #(prep-val label path %) l)}))

(def lazy-sample 1000)

(defmethod prep-val :seq [label path s]
  (let [selection (take lazy-sample s)]
    (if (= (count selection) lazy-sample)
      {:type :summary
       :val (str "(" lazy-sample "+ items, click to load 0-" lazy-sample ")")
       :actions [[:set-path label path]]}
      (if (too-long-for-inline? s)
        {:type :summary :val (summarize "(" s ")")}
        {:type :seq :val (map #(prep-val label path %) s)}))))

(defn- key-vals [v]
  (cond
    (map? v) (let [v (sort-keys v)]
               [(map first v) (map second v)])
    (seq? v) (let [selection (take lazy-sample v)
                   kvs [(range (count selection)) selection]]
               (if (< (count selection) lazy-sample)
                 kvs
                 [(concat (first kvs) ["..."]) (concat (second kvs) ["Truncated to first " lazy-sample " elements"])]))
    :default [(range (count v)) v]))

(defmulti get* (fn [data path] path))

(defn- base64json [s]
  (-> s js/atob JSON.parse (js->clj :keywordize-keys true)))

(defmethod get* :gadget/JWT [data path]
  (when (string? data)
    (let [[header data sig] (str/split data #"\.")]
      {:header (base64json header)
       :data (base64json data)
       :signature sig})))

(defmethod get* :default [data path]
  (if (and (seq? data) (number? path))
    (nth data path)
    (get data path)))

(defn get-in* [data path]
  (if-let [p (first path)]
    (recur (get* data p) (rest path))
    data))

(defn- prep-top-level-val [label path v]
  (assoc (prep-val label path v) :copyable (pr-str v)))

(defn prepare-data [{:keys [label path ref]}]
  (let [v (get-in* (some-> ref deref) path)]
    {:path (prepare-path (concat [label] path))
     :data (when v
             (let [[ks vs] (key-vals v)]
               (map vector (map prep-key ks) (map #(prep-top-level-val label [%1] %2) ks vs))))
     :copyable (when v (pr-str v))}))

(defn prepare [state]
  {:atoms (map prepare-data (:atoms state))})

(defn render []
  (render-data (pr-str {:type :render
                        :data (prepare @store)})))

(defn inspect [label ref]
  (add-watch ref :gadget/inspector render)
  (swap! store update :atoms #(conj % {:label label :ref ref :path []}))
  render
  nil)

(defn create-atom [label & [val]]
  (let [ref (atom val)]
    (inspect label ref)
    ref))
