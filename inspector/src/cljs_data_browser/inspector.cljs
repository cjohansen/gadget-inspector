(ns cljs-data-browser.inspector
  (:require [cljs-data-browser.actions :as actions]
            [clojure.walk :as w]
            [dumdom.core :as q]
            [dumdom.dom :as d]))

(defn trigger-actions [actions]
  (doseq [[action & args] actions]
    (actions/exec-action (pr-str {:action action :args args}))))

(defn action-fn [actions]
  (when actions
    (fn [e]
      (.preventDefault e)
      (.stopPropagation e)
      (trigger-actions actions))))

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

(q/defcomponent String [s]
  (code {:style {:color "#690"}} s))

(q/defcomponent Number [n]
  (code {:style {:color "#905"}} n))

(q/defcomponent KeywordValue [kw]
  (code {:style {:color "#c80000"}} kw))

(q/defcomponent Boolean [b]
  (code {:style {:color "#3424fb" :fontWeight "bold"}} b))

(q/defcomponent Date [d]
  (code {:style {:fontWeight "bold"}} d))

(q/defcomponent Literal [{:keys [prefix str]}]
  [:span {}
   (code {:style {:fontWeight "bold"}} prefix)
   " "
   (String str)])

(q/defcomponent InlineCollection [{:keys [brackets xs]}]
  [:span {}
   [:strong {} (first brackets)]
   (interpose " " xs)
   [:strong {} (second brackets)]])

(q/defcomponent Link [v]
  [:strong {:style button-styles}
   v])

(q/defcomponent CopyButton [actions]
  (d/div {:style {:padding "0 12px"}}
    (d/span {:style {:cursor "pointer"
                     :border "1px solid #999"
                     :borderRadius "3px"
                     :padding "2px 3px"}
             :title "Copy data to clipboard"
             :className "button"
             :onClick (action-fn actions)}
      "copy")))

(q/defcomponent Entry
  "An entry is one key/value pair (or index/value pair), formatted appropriately
  for their types"
  [{:keys [k v actions]}]
  (d/tr {:onClick (action-fn (:go actions))
         :style (when (:go actions) {:cursor "pointer"})}
    (d/td {:style {:padding "5px 12px" :whiteSpace "nowrap"}}
      k)
    (d/td {:style {:padding "5px"
                   :position "relative"
                   :width "100%"}}
      v
      (d/div {:style {:position "absolute"
                      :right 0
                      :top 5
                      :transition "opacity 0.25s"}
              :className "copy-btn"}
        (some-> (:copy actions) CopyButton)))))

(q/defcomponent DataPath
  "The heading and current path in the data. When browsing nested maps and lists,
   the path component will display the full path from the root of the map/seq,
   with navigation options along the way."
  [path]
  [:p {:style {:padding "0 0 0 12px"
               :margin "8px 0"}}
   path])

(q/defcomponent Tab [{:keys [text active? actions]}]
  [:span {:style (merge {:padding "4px 12px"
                         :display "inline-block"}
                        (when active?
                          {:borderBottom "2px solid #2376ef"
                           :marginBottom "-1px"
                           :paddingBottom "3px"}))
          :className (when actions "tab-clickable")
          :onClick (action-fn actions)}
   text])

(q/defcomponent Header
  :keyfn #(str (-> % :path first :text) "-header")
  [{:keys [path actions tabs]}]
  [:div
   [:div {:style {:background "#f3f3f3"
                  :display "flex"}}
    (map Tab tabs)]])

(q/defcomponent Browser
  :keyfn :key
  [{:keys [data path actions]}]
  [:div
   [:div {:style {:display "flex"
                  :justifyContent "space-between"
                  :alignItems "center"
                  :borderBottom "1px solid #ccc"}}
    [DataPath path]
    [CopyButton (:copy actions)]]
   [:table {:style {:borderCollapse "collapse"
                    :width "100%"}}
    [:tbody {} (map Entry data)]]])

(q/defcomponent Transaction [details]
  [:div {:style {:background "#fff"}}
   details])

(q/defcomponent TxList
  :keyfn :key
  [txes]
  [:table {:style {:borderCollapse "collapse"
                   :width "100%"}}
   [:tbody {}
    (map #(d/tr {}
            (d/td {:style {:whiteSpace "nowrap"
                           :width "100%"}}
              [:div {:style {:padding "5px 5px 5px 12px"}
                     :onClick (action-fn (:actions %))}
               (:summary %)]
              (when-let [details (:details %)]
                [Transaction details]))) txes)]])

(def component-map
  {:gadget/button Button
   :gadget/browser Browser
   :gadget/tx-list TxList
   :gadget/string String
   :gadget/number Number
   :gadget/keyword KeywordValue
   :gadget/boolean Boolean
   :gadget/literal Literal
   :gadget/inline-coll InlineCollection
   :gadget/link Link
   :gadget/date Date
   :gadget/code code})

(q/defcomponent DataPanel [data]
  [:div {:style {:borderBottom "1px solid #ccc"}}
   (Header data)
   (when-let [hiccup (:hiccup data)]
     [:div {:style {:borderTop "1px solid #ccc"}}
      (w/postwalk #(get component-map % %) (:hiccup data))])])

(q/defcomponent Inspector [{:keys [data]}]
  [:div {:className "inspector"
         :style {:fontSize "12px"
                 :fontFamily "helvetica neue, lucida grande, sans-serif"
                 :lineHeight "1.5"
                 :color "#333"
                 :textShadow "0 1px 0 rgba(255, 255, 255, 0.6)"}}
   (map DataPanel data)])
