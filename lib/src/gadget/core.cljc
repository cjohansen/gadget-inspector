(ns gadget.core
  (:require #?(:cljs [cljs.reader :as reader])
            [clojure.string :as str]
            [gadget.datafy :as datafy]
            [gadget.actions :as actions]
            [gadget.std :refer [debounce]]))

(defmulti render-data identity)

(defonce enabled? (atom true))
(defonce store (atom {:data {}}))

(defn deserialize [payload]
  #?(:cljs (reader/read-string payload)
     :clj (read-string payload)))

(def pending-action? (atom false))

(defn action [payload]
  (let [{:keys [action args]} (deserialize payload)]
    (reset! pending-action? true)
    (actions/exec-action store action args)))

;; Sorting

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

(defn key-order [ks]
  (let [ks (reverse ks)]
    (fn [[k v]] (- (.indexOf ks k)))))

;; Rendering

(defmulti render (fn [view data] [view (:type data)]))

(defn render-with-view [view label path raw]
  (render view {:raw raw
                :type (datafy/synthetic-type raw)
                :data (datafy/datafy raw)
                :label label
                :path path}))

(defn prep-browser-entries [label path entries]
  (->> entries
       (map (fn [[k v]]
              (let [target-path (conj path k)]
                {:k (render-with-view :inline label path k)
                 :v (render-with-view :inline label target-path v)
                 :actions {:go [[:set-path label target-path]]
                           :copy [[:copy-to-clipboard label target-path]]}})))))

(defn- browser-data [label path data]
  [:gadget/browser {:key (str label "-browser")
                    :data (prep-browser-entries label path data)}])

(defprotocol Browsable
  :extend-via-metadata true
  (entries [data] "Return a sorted seq of key value pairs for browsing"))

(extend-type #?(:cljs cljs.core/PersistentVector
                :clj clojure.lang.PersistentVector)
  Browsable
  (entries [m] (->> m (map-indexed vector) (sort-by first))))

(extend-type #?(:cljs cljs.core/List
                :clj clojure.lang.PersistentList)
  Browsable
  (entries [m] (->> m (map-indexed vector) (sort-by first))))

(def lazy-sample 1000)

;; TODO: Add action to navigate further
(extend-type #?(:cljs cljs.core/LazySeq
                :clj clojure.lang.LazySeq)
  Browsable
  (entries [s] (->> s
                    (take lazy-sample)
                    (map-indexed vector)
                    (sort-by first))))

(defmethod render [:inline :keyword] [_ {:keys [raw]}]
  [:gadget/keyword (pr-str raw)])

(defmethod render [:inline :number] [_ {:keys [raw]}]
  [:gadget/number (pr-str raw)])

(defmethod render [:inline :boolean] [_ {:keys [raw]}]
  [:gadget/boolean (pr-str raw)])

(defmethod render [:inline :string] [_ {:keys [raw]}]
  [:gadget/string (pr-str raw)])

(defmethod render [:inline :nil] [_ {:keys [raw]}]
  [:gadget/code {} "nil"])

(defmethod render [:inline :symbol] [_ {:keys [raw]}]
  [:gadget/code {} raw])

(defn- constructor [v]
  (second (re-find #"function (.*)\(" (str (type v)))))

(defmethod render [:inline :object] [_ {:keys [raw]}]
  [:gadget/code {}
   "object[" [:strong {} (constructor raw)] "]"
   #?(:cljs (when-not (= (.. js/Object -prototype -toString) (.-toString raw))
              [:span "{" [:gadget/string (str "\"" (.toString raw) "\"")] "}"]))])

(defmethod render [:inline :date] [_ {:keys [raw]}]
  (let [[prefix str] (str/split (pr-str raw) #" ")]
    [:gadget/literal {:prefix prefix :str str}]))

(def inline-length-limit 120)

(defn- too-long-for-inline? [v]
  (< inline-length-limit
     (count (pr-str v))))

(defn- inflect [n w]
  (if (= n 1)
    w
    (str w "s")))

(defn- summarize [pre c post & [w]]
  (let [num (count c)
        types (into #{} (map datafy/synthetic-type c))
        w (or w (if (= 1 (count types)) (name (first types)) "item"))]
    (str pre num " " (inflect num w) post)))

(defmethod render [:inline :set] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (summarize "#{" raw "}")]]
    [:gadget/inline-coll {:brackets ["#{" "}"]
                          :xs (->> raw
                                   sort-vals
                                   (map #(render-with-view :inline label path %)))}]))

(defmethod render [:inline :vector] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (summarize "[" raw "]")]]
    [:gadget/inline-coll {:brackets ["[" "]"]
                          :xs (map #(render-with-view :inline label path %) raw)}]))

(defmethod render [:inline :list] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (summarize "'(" raw ")")]]
    [:gadget/inline-coll {:brackets ["'(" ")"]
                          :xs (map #(render-with-view :inline label path %) raw)}]))

(defmethod render [:inline :map] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    (let [ks (map first (sort-keys raw))]
      (if (too-long-for-inline? ks)
        [:gadget/link [:gadget/code {} (summarize "{" ks "}" "key")]]
        [:gadget/code {}
         [:gadget/inline-coll
          {:brackets ["{" "}"]
           :xs (map #(render-with-view :inline label (conj path %) %) ks)}]]))
    [:gadget/inline-coll
     {:brackets ["{" "}"]
      :xs (->> raw
               sort-keys
               (map (fn [[k v]]
                      [(render-with-view :inline label path k)
                       " "
                       (render-with-view :inline label (conj path k) v)]))
               (interpose ", ")
               (mapcat identity))}]))

(defmethod render [:inline :seq] [_ {:keys [label path raw]}]
  (let [selection (take lazy-sample raw)]
    (cond
      (= (count selection) lazy-sample)
      [:gadget/link
       [:gadget/code {}
        (str "(" lazy-sample "+ items, click to load 0-" lazy-sample ")")]]

      (too-long-for-inline? raw)
      [:gadget/link [:gadget/code {} (summarize "(" raw ")")]]

      :default
      [:gadget/inline-coll
       {:brackets ["(" ")"]
        :xs (->> raw
                 sort-vals
                 (map-indexed #(render-with-view :inline label (conj path %1) %2)))}])))

(defmethod render :default [view v]
  (let [t (datafy/symbolic-type (:data v))]
    (cond
      (not= t (:type v))
      (render view (assoc v :type t))

      (and (= :full view) (satisfies? Browsable (:data v)))
      (browser-data (:label v) (:path v) (entries (:data v)))

      ;; Handle the map default here instead of implementing the Browsable
      ;; protocol for maps, because ClojureScript currently has a bug where you
      ;; cannot override a protocol implementation on a type from metadata. This
      ;; way, you can implement Browsable from metadata, and have that
      ;; implementation override this default behavior.
      (and (= :full view) (map? (:data v)))
      (browser-data (:label v) (:path v) (sort-keys (:data v)))

      :default
      [:span {:style {:padding "6px"}} (pr-str (:raw v))])))

(defn- prepare-path [path-elems]
  (loop [[x & xs] path-elems
         path []
         res []]
    (if (seq xs)
      (let [path (conj path x)]
        (recur (vec xs) path (conj res {:text (str x)
                                        :actions {:go [[:set-path (first path) (into [] (rest path))]]}})))
      (if x
        (conj res {:text (str x)})
        res))))

(def rendered (atom {:data nil :hiccup nil :meta nil}))

(defn- get-data-hiccup [label path raw data]
  (let [state @rendered]
    (if (and (= data (:data state)) (= (meta data) (:meta state)))
      (:hiccup state)
      (let [hiccup (render :full {:label label
                                  :path path
                                  :raw raw
                                  :data data
                                  :type (datafy/synthetic-type raw)})]
        (reset! rendered {:data data :hiccup hiccup :meta (meta data)})
        hiccup))))

(defn prepare-data [window {:keys [label path ref data]}]
  (let [raw (datafy/nav-in (or (some-> ref deref) data) path)]
    {:path (prepare-path (concat [label] path))
     :hiccup (get-data-hiccup label path raw (datafy/datafy raw))
     :actions {:copy [[:copy-to-clipboard label path]]}}))

(defn prepare [state]
  {:data (->> (:data state)
              (sort-by first)
              (map (comp #(prepare-data (:window state) %) second)))})

(defn render-data-now [f]
  (render-data f))

(def render-data-debounced (atom (debounce render-data-now 250)))

(defn set-render-debounce-ms! [ms]
  (reset!
   render-data-debounced
   (if (= ms 0)
     render-data-now
     (debounce render-data-now ms))))

(defn render-inspector []
  (when @enabled?
    (let [render-fn (if @pending-action? render-data-now @render-data-debounced)]
      (when @pending-action?
        (reset! pending-action? false))
      (render-fn
       (fn []
         (pr-str {:type :render
                  :data (prepare @store)}))))))

(add-watch store :gadget/inspector (fn [_ _ _ _] (render-inspector)))

(defn- atom? [ref]
  (instance? #?(:cljs Atom
                :clj clojure.lang.Atom) ref))

(defn inspectable? [ref {:keys [inspectable?]}]
  (or (not (ifn? inspectable?))
      (inspectable? (if (atom? ref) @ref ref))))

(defn now []
  #?(:cljs (js/Date.)
     :clj (java.util.Date.)))

(defn create-tx [label old-state new-state]
  (let [prev-tx-data (get-in @store [:data label :gadget.tx/data])
        limit (get-in @store [:config label :tx-limit] 100)
        tx-data (:gadget.tx/data new-state)
        valid-tx? (not= prev-tx-data tx-data)
        tx (cond-> {:gadget.tx/instant (now)
                    :gadget.tx/state new-state}
             valid-tx? (assoc :gadget.tx/data tx-data))]
    (swap! store update-in [:data label] update :txes (fn [txes]
                                                        (take limit (conj txes tx))))))

(defn inspect [label ref & [opts]]
  (when (atom? ref)
    (add-watch ref :gadget/inspector (fn [_ _ old-state new-state]
                                       (create-tx label old-state new-state)
                                       (when (inspectable? new-state opts)
                                         (render-inspector)))))
  (when (inspectable? ref opts)
    (swap! store update :data assoc label (merge {:label label :path []}
                                                 (select-keys (get-in @store [:data label]) [:path])
                                                 (if (atom? ref)
                                                   {:ref ref}
                                                   {:data ref}))))
  nil)

(defn create-atom [label & [val]]
  (let [ref (atom val)]
    (inspect label ref)
    ref))

(defn pause! []
  (reset! enabled? false))

(defn resume! []
  (reset! enabled? true)
  (render-inspector))
