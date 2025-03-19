(ns gadget.core
  (:require #?(:cljs [cljs.reader :as reader])
            [clojure.string :as str]
            [gadget.datafy :as datafy]
            [gadget.actions :as actions]
            [gadget.browsable :as browsable]
            [gadget.std :refer [debounce pad]]))

(defmulti render-data identity)

(defonce enabled? (atom true))
(defonce store (atom {:data {}}))

(defn deserialize [payload]
  #?(:cljs (reader/read-string payload)
     :clj (read-string payload)))

(def pending-action?
  "This atom is used to bypass the debounce on render when processing actions from
  the inspector UI. It should probably be removed in favor of `action` calling
  on render without debouncing in a more direct fashion."
  (atom false))

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

(def bespoke-labels
  {:gadget/value 'Value
   :gadget/type 'Type})

(defn prep-browser-entries [label path entries]
  (->> entries
       (map (fn [entry]
              (let [[k v] entry
                    target-path (conj path k)]
                {:k (when-not (-> entry meta :synthetic-key?)
                      (render-with-view :inline label path (get bespoke-labels k k)))
                 :v (render-with-view :inline label target-path v)
                 :actions (when-not (contains? bespoke-labels k)
                            {:go [[:assoc-state [label :path] target-path]]
                             :copy [[:copy-to-clipboard label target-path]]})})))))

(defn- prepare-path [path-elems]
  (loop [[x & xs] path-elems
         path []
         res [:div {}]]
    (if (seq xs)
      (let [path (conj path x)
            button {:actions [[:assoc-state [(first path) :path] (into [] (rest path))]]
                    :content (str x)}]
        (recur (vec xs) path (conj res [:gadget/button button] " ")))
      (if x
        (conj res [:strong {} (str x)] " ")
        res))))

(defn- browser-data [label path metadata data]
  [:gadget/browser {:key (str label "-browser")
                    :metadata (prep-browser-entries label path metadata)
                    :data (prep-browser-entries label path data)
                    :path (prepare-path (concat [label] path))
                    :actions {:copy [[:copy-to-clipboard label path]]}}])

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
  [:gadget/code {} (str raw)])

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

(defmethod render [:inline :uuid] [_ {:keys [raw]}]
  (let [[prefix str] (str/split (pr-str raw) #" ")]
    [:gadget/literal {:prefix prefix :str str}]))

(def inline-length-limit 120)

(defn too-long-for-inline? [v]
  (< inline-length-limit
     (count (pr-str v))))

(defn- inflect [n w]
  (if (= n 1)
    w
    (if (str/ends-with? w "y")
      (str (str/join (drop-last 1 w)) "ies")
      (str w "s"))))

(defn summarize [pre c post & [w]]
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
                                   (map #(render-with-view :summary label path %)))}]))

(defmethod render [:inline :vector] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (summarize "[" raw "]")]]
    [:gadget/inline-coll {:brackets ["[" "]"]
                          :xs (map #(render-with-view :summary label path %) raw)}]))

(defmethod render [:inline :list] [_ {:keys [raw label path]}]
  (if (too-long-for-inline? raw)
    [:gadget/link [:gadget/code {} (summarize "'(" raw ")")]]
    [:gadget/inline-coll {:brackets ["'(" ")"]
                          :xs (map #(render-with-view :summary label path %) raw)}]))

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
  (let [lazy-sample browsable/lazy-sample
        selection (take lazy-sample raw)]
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
                 (map-indexed #(render-with-view :summary label (conj path %1) %2)))}])))

(defmethod render :default [view v]
  (if (= :summary view)
    (render :inline v)
    (let [t (datafy/symbolic-type (:data v))]
      (cond
        (not= t (:type v))
        (render view (assoc v :type t))

        (= :full view)
        (->> (cond
               (satisfies? browsable/Browsable (:data v))
               (browsable/entries (:data v))

               ;; Handle the map default here instead of implementing the Browsable
               ;; protocol for maps, because ClojureScript currently has a bug where you
               ;; cannot override a protocol implementation on a type from metadata. This
               ;; way, you can implement Browsable from metadata, and have that
               ;; implementation override this default behavior.
               (map? (:data v))
               (sort-keys (:data v))

               :default
               [[:gadget/type (let [t (datafy/symbolic-type (:data v))]
                                (if (= :object t)
                                  (constructor (:data v))
                                  t))]
                [:gadget/value (:data v)]])

             (browser-data (:label v) (:path v) (meta (:data v))))

        :default
        [:span {:style {:padding "6px"}} (pr-str (:raw v))]))))

(def rendered (atom {:data nil :hiccup nil :meta nil}))

(defn- browser-hiccup [label path raw data]
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


(defn same-date? [a b]
  (and (= (.getYear a) (.getYear b))
       (= (.getMonth a) (.getMonth b))
       (= (.getDate a) (.getDate b))))

(defn tx-hiccup [{:keys [label txes tx-expand]}]
  (let [now #?(:cljs (js/Date.)
               :clj (java.util.Date.))
        expanded (set tx-expand)]
    [:gadget/tx-list
     (map #(let [{:gadget.tx/keys [instant]} %
                 expanded? (contains? expanded (:gadget.tx/id %))]
             {:summary
              [:div {}
               [:strong {}
                (when-not (same-date? instant now)
                  (str (+ 1900 (.getYear instant)) "-"
                       (pad (inc (.getMonth instant))) "-"
                       (pad (.getDate instant)) " "))
                (str (pad (.getHours instant)) ":"
                     (pad (.getMinutes instant)) ":"
                     (pad (.getSeconds instant)) " ")]
               (some->> (:gadget.tx/data %)
                        (render-with-view :inline "tx" []))]
              :actions [[:assoc-state [label :tx-expand] (if expanded?
                                                           (remove #{(:gadget.tx/id %)} expanded)
                                                           (conj expanded (:gadget.tx/id %)))]]
              :details (when expanded?
                         #_(browser-hiccup "Transaction data" [(:gadget.tx/id %)] % %)
                         [:gadget/browser
                          {:key (str "tx-browser")
                           ;;:metadata (prep-browser-entries label path metadata)
                           :data (prep-browser-entries "TX" [] (sort-keys %))
                           :path (prepare-path (concat ["TX"] []))
                           :actions {:copy [[:copy-to-clipboard "TX" []]]}}])
              })
          txes)]))

(defn prepare-data [window {:keys [label path ref data txes] :as state}]
  (let [raw (datafy/nav-in (or (some-> ref deref) data) path)
        expanded? (get state :expanded? true)
        current-tab (when expanded? (get state :current-tab :browser))]
    {:tabs (concat
            [{:text (if expanded? [:strong label] label)
              :actions [[:assoc-state [label :expanded?] (not expanded?)]]}]
            (when expanded?
              [{:text "Browse"
                :active? (= :browser current-tab)
                :actions [[:assoc-state [label :current-tab] :browser]]}])
            (when (and expanded? ref ;;false
                       )
              [{:text "Transactions"
                :actions [[:assoc-state [label :current-tab] :txes]]
                :active? (= current-tab :txes)}])
            [{:text "Close"
              :actions [[:uninspect label]]}]
            (when ref
              [{:text "Unsubscribe"
                :actions [[:unsubscribe label]]}]))
     :hiccup (when expanded?
               (if (= :txes current-tab)
                 (tx-hiccup state)
                 (browser-hiccup label path raw (datafy/datafy raw))))}))

(defn prepare [state]
  {:data (->> (:data state)
              (sort-by first)
              (map (comp #(prepare-data (:window state) %) second)))})

(defn render-data-now [f]
  (render-data f))

(def render-data-debounced (atom (debounce #'render-data-now 250)))

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
  #?(:cljs (satisfies? IAtom ref)
     :clj (instance? clojure.lang.IAtom ref)))

(defn inspectable? [ref {:keys [inspectable?]}]
  (or (not (ifn? inspectable?))
      (inspectable? (if (atom? ref) @ref ref))))

(defn now []
  #?(:cljs (js/Date.)
     :clj (java.util.Date.)))

(defn create-tx [label old-state new-state]
  (let [limit (get-in @store [:config label :tx-limit] 100)
        tx-data (:gadget.tx/data new-state)
        valid-tx? (not= tx-data (:gadget.tx/data old-state))
        tx (cond-> {:gadget.tx/instant (now)
                    :gadget.tx/state new-state
                    :gadget.tx/id (get-in @store [:data label :tx-count] 0)}
             valid-tx? (assoc :gadget.tx/data tx-data))]
    (swap! store update-in [:data label] (fn [d]
                                           (-> d
                                               (update :tx-count inc)
                                               (update :txes #(take limit (conj % tx))))))))

(defn inspect [label ref & [opts]]
  (when (atom? ref)
    (add-watch ref :gadget/inspector (fn [_ _ old-state new-state]
                                       (create-tx label old-state new-state)
                                       (when (inspectable? new-state opts)
                                         (render-inspector))))
    (swap! store assoc-in [:refs label] ref))

  (when (atom? ref)
    (create-tx label nil @ref))

  (when (inspectable? ref opts)
    (swap! store update :data assoc label (merge
                                           {:path [] :label label :tx-count 0}
                                           (get-in @store [:data label])
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

(defmethod actions/exec-action :ping [store _ [k v]]
  (render-inspector))
