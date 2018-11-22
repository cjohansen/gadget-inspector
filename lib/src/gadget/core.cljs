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

(def inline-length-limit 200)

(defn- too-long-for-inline? [v]
  (< inline-length-limit
     (.-length (pr-str v))))

(defmulti prep-val vtype)

(defmethod prep-val :default [v]
  {:val (pr-str v) :type (vtype v)})

(def re-jwt #"^[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*$")

(defmethod prep-val :string [s]
  (cond
    (re-find re-jwt s)
    {:val (str (first (str/split s #"\.")) "...")
     :type :jwt
     :actions [[:set-path "App state" [:gadget/JWT]]]}

    :default {:type :string :val (pr-str s)}))

(defmethod prep-val :object [o]
  (let [constructor (constructor o)]
    {:val (str "object[" constructor "]")
     :type :object
     :constructor constructor}))

(defn- inflect [n w]
  (if (= n 1)
    w
    (str w "s")))

(defn- summarize [pre c post & [w]]
  (let [num (count c)
        types (into #{} (map vtype c))
        w (or w (if (= 1 (count types)) (name (first types)) "item"))]
    (str pre num " " (inflect num w) post)))

(defmethod prep-val :map [m]
  (if (too-long-for-inline? m)
    (let [ks (keys m)]
      (assoc
       (if (too-long-for-inline? ks)
         {:type :summary :val (summarize "{" ks "}" "key")}
         {:type :map-keys :val (sort-by str ks)})
       :actions [[:set-path "???" []]]))
    {:type :map :val (->> (map vector (map prep-key (keys m)) (map prep-val (vals m)))
                          (into {}))}))

(defmethod prep-val :set [s]
  (if (too-long-for-inline? s)
    {:type :summary :val (summarize "#{" s "}") :actions [[:set-path "???" []]]}
    {:type :set :val s}))

(defmethod prep-val :vector [v]
  (if (too-long-for-inline? v)
    {:type :summary :val (summarize "[" v "]") :actions [[:set-path "???" []]]}
    {:type :vector :val v}))

(defmethod prep-val :list [l]
  (if (too-long-for-inline? l)
    {:type :summary :val (summarize "(" l ")") :actions [[:set-path "???" []]]}
    {:type :list :val l}))

(def lazy-sample 1000)

(defmethod prep-val :seq [s]
  (let [selection (take lazy-sample s)]
    (if (= (count selection) lazy-sample)
      {:type :summary :val (str "(" lazy-sample "+ items, click to load 0-" lazy-sample ")")}
      (if (too-long-for-inline? s)
        {:type :summary :val (summarize "(" s ")")}
        {:type :seq :val s}))))

(defn- key-vals [v]
  (cond
    (map? v) [(keys v) (vals v)]
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

(defn prepare-data [{:keys [label path ref]}]
  {:path (prepare-path (concat [label] path))
   :data (when-let [v (get-in* (some-> ref deref) path)]
           (let [[ks vs] (key-vals v)]
             (->> (map vector (map prep-key ks) (map prep-val vs))
                  (into {}))))})

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
