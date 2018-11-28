(ns cljs-data-browser.inspector
  (:require [cljs-data-browser.actions :as actions]
            [quiescent.core :as q]
            [quiescent.dom :as d]))

(defn trigger-actions [actions]
  (doseq [[action & args] actions]
    (actions/exec-action (pr-str {:action action :args args}))))

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

(def button-styles
  {:color "#3424fb"
   :textDecoration "underline"
   :cursor "pointer"})

(q/defcomponent Button [{:keys [actions content title]}]
  (d/span {:style button-styles
           :title title
           :onClick #(trigger-actions actions)}
    content))

(q/defcomponent InlineSymbol [s]
  (code {:style (type-styles (:type s))}
        (:val s)))

(declare ComplexSymbol)

(q/defcomponent InlineMap [m]
  (apply code {}
         (concat [(d/strong {} "{")]
                 (->> m
                      (map (fn [[k v]] [(InlineSymbol k) " " (ComplexSymbol v)]))
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
    (d/strong {} "JWT: ")
    (InlineSymbol {:type :string :val (:val token)})))

(q/defcomponent MapKeys [k]
  (apply d/span {}
         (flatten
          (concat [(d/strong {} "{")]
                  (interpose " " (map ComplexSymbol (:val k)))
                  [(d/strong {} "}")]))))

(q/defcomponent ComplexSymbol [sym]
  (cond
    (= (:type sym) :jwt) (JWT sym)
    (= (:type sym) :map-keys) (MapKeys sym)

    :default
    (d/span {}
      (cond
        (= (:type sym) :summary) (d/strong {:style button-styles} (code {} (:val sym)))
        (= (:type sym) :map) (InlineMap (:val sym))
        (#{:list :seq :vector :set} (:type sym)) (InlineCollection sym)
        :default (code {:style (type-styles (:type sym))} (:val sym))))))

(q/defcomponent CopyButton [actions]
  (d/div {:style {:padding "0 5px"}}
    (d/span {:style {:cursor "pointer"
                     :border "1px solid #999"
                     :borderRadius "3px"
                     :padding "2px 3px"
                     :background "#f9f9f9"}
             :title "Copy data to clipboard"
             :onClick (fn [e]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (trigger-actions actions))}
      "copy")))

(q/defcomponent Entry
  "An entry is one key/value pair (or index/value pair), formatted appropriately
  for their types"
  [[k v]]
  (d/tr {:onClick (when-let [actions (:actions v)]
                    (fn [e]
                      (trigger-actions (:go actions))))
         :style (when (:actions v) {:cursor "pointer"})}
    (d/td {:style {:padding "5px" :whiteSpace "nowrap" :minWidth "200px"}} (InlineSymbol k))
    (d/td {:style {:padding "5px"
                   :position "relative"}}
      (ComplexSymbol v)
      (d/div {:style {:position "absolute"
                      :right 0
                      :top 5
                      :transition "opacity 0.25s"}
              :className "copy-btn"}
        (CopyButton (-> v :actions :copy))))))

(q/defcomponent DataPath
  "The heading and current path in the data. When browsing nested maps and lists,
   the path component will display the full path from the root of the map/seq,
   with navigation options along the way."
  [path]
  (apply d/p {:style {:padding "0 0 0 5px"}}
         (interpose " "
                    (map (fn [{:keys [text actions]}]
                           (if actions
                             (Button {:actions (:go actions) :content text})
                             (d/strong {} text)))
                         path))))

(q/defcomponent Header
  :keyfn #(str (-> % :path first :text) "-header")
  [{:keys [path actions]} callback]
  (d/thead {}
    (d/tr {}
      (d/td {:colSpan 2}
        (d/div {:style {:display "flex"
                        :justifyContent "space-between"
                        :alignItems "center"}}
          (DataPath path)
          (CopyButton (:copy actions)))))))

(q/defcomponent Browser
  :keyfn #(str (-> % :path first :text) "-browser")
  [{:keys [data]} callback]
  (apply d/tbody {} (map Entry data)))

(q/defcomponent Inspector [{:keys [data]}]
  (d/div {:className "inspector"
          :style {:fontSize "12px"
                  :fontFamily "helvetica neue, lucida grande, sans-serif"
                  :lineHeight "1.5"
                  :color "#333"
                  :textShadow "0 1px 0 rgba(255, 255, 255, 0.6)"}}
    (d/table {:style {:borderCollapse "collapse"
                      :width "100%"}}
      (mapcat vector
              (map Header data)
              (map Browser data)))))
