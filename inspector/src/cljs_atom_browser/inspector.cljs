(ns cljs-atom-browser.inspector
  (:require [quiescent.core :as q]
            [quiescent.dom :as d]))

(defn- type-styles [t]
  (cond
    (= t :string) {:color "#690"}
    (= t :number) {:color "#905"}
    (= t :keyword) {:color "#c80000"}
    (= t :map-keys) {:color "#c80000"}
    (= t :boolean) {:color "#3424fb" :fontWeight "bold"}))

(def code-styles
  {:fontFamily "menlo, lucida console, monospace"})

(defn code [attrs & children]
  (apply d/code (assoc attrs :style (merge code-styles (:style attrs))) children))

(q/defcomponent Button [{:keys [actions content title]}]
  (d/span {:style {:color "#3424fb"
                   :text-decoration "underline"
                   :cursor "pointer"}
           :title title}
    content))

(q/defcomponent InlineSymbol [s]
  (code {:style (type-styles (:type s))}
        (:val s)))

(declare ComplexSymbol)

(q/defcomponent InlineMap [m]
  (apply code {}
         (concat [(d/strong {} "{")]
                 (->> (keys m)
                      (sort-by (comp str :val))
                      (map (fn [k] [(InlineSymbol k) " " (ComplexSymbol (m k))]))
                      (interpose ", ")
                      flatten)
                 [(d/strong {} "}")])))

(def brackets
  {:vector ["[" "]"]
   :list ["(" ")"]
   :seq ["(" ")"]
   :set ["#{" "}"]})

(q/defcomponent InlineCollection [c]
  (let [[pre post] (brackets (:type c))]
    (apply
     d/span {}
     (concat [(d/strong {} pre)]
             (interpose " " (map ComplexSymbol (:val c)))
             [(d/strong {} post)]))))

(q/defcomponent JWT [token]
  (d/span {}
    (InlineSymbol {:type :string :val (:val token)})
    " "
    (Button {:actions (:actions token)
             :content "Decode JWT"
             :title "Decode and browse JSON Web Token"})))

(q/defcomponent ComplexSymbol [sym]
  (cond
    (= (:type sym) :jwt) (JWT sym)

    :default
    (let [content (cond
                    (= (:type sym) :summary) (code {:style (type-styles (:type sym))} (:val sym))
                    (= (:type sym) :map) (InlineMap (:val sym))
                    (#{:list :seq :vector :set} (:type sym)) (InlineCollection sym)
                    :default (code {:style (type-styles (:type sym))} (:val sym)))]
      (d/span {}
        (if-let [actions (:actions sym)]
          (Button {:actions actions :content content :title (:title sym)})
          content)))))

(q/defcomponent MapItem
  "A map entry is one key/value pair, formatted appropriately for their types"
  [[k v]]
  (d/tr {}
    (d/td {:style {:padding "5px"}} (InlineSymbol k))
    (d/td {:style {:padding "5px"}} (ComplexSymbol v))))

(q/defcomponent MapPath
  "The heading and current path in the map. When browsing nested maps and lists,
   the path component will display the full path from the root of the map, with
   navigation options along the way."
  [path]
  (apply d/p {:style {:padding "0 0 0 5px"}}
         (interpose " "
                    (map (fn [{:keys [text actions]}]
                           (if actions
                             (Button {:actions actions :content text})
                             (d/strong {} text)))
                         path))))

(q/defcomponent MapBrowser [{:keys [path data]} callback]
  (d/div {:style {:marginBottom "10px"}}
    (MapPath path)
    (d/table {:style {:borderCollapse "collapse"
                      :width "100%"}}
      (apply d/tbody {} (map MapItem data)))))

(q/defcomponent Inspector [{:keys [atoms]}]
  (d/div {:className "inspector"
          :style {:fontSize "12px"
                  :fontFamily "helvetica neue, lucida grande, sans-serif"
                  :lineHeight "1.5"
                  :color "#333"
                  :textShadow "0 1px 0 rgba(255, 255, 255, 0.6)"}}
    (map MapBrowser atoms)))
