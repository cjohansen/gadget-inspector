(ns cljs-data-browser.inspector
  (:require [cljs-data-browser.actions :as actions]
            [clojure.walk :as w]
            [dumdom.core :as q]
            [dumdom.dom :as d]))

(defn trigger-actions [actions]
  (doseq [[action & args] actions]
    (actions/exec-action (pr-str {:action action :args args}))))

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
  (d/div {:style {:padding "0 5px"}}
    (d/span {:style {:cursor "pointer"
                     :border "1px solid #999"
                     :borderRadius "3px"
                     :padding "2px 3px"}
             :title "Copy data to clipboard"
             :className "button"
             :onClick (fn [e]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (trigger-actions actions))}
      "copy")))

(q/defcomponent Entry
  "An entry is one key/value pair (or index/value pair), formatted appropriately
  for their types"
  [{:keys [k v actions]}]
  (d/tr {:onClick (when-let [actions (:go actions)]
                    (fn [e]
                      (trigger-actions actions)))
         :style (when (:go actions) {:cursor "pointer"})}
    (d/td {:style {:padding "5px 15px 5px 5px" :whiteSpace "nowrap"}}
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
        (CopyButton (:copy actions))))))

(q/defcomponent DataPath
  "The heading and current path in the data. When browsing nested maps and lists,
   the path component will display the full path from the root of the map/seq,
   with navigation options along the way."
  [path]
  (apply d/p {:style {:padding "0 0 0 5px"
                      :margin "8px 0"}}
         (interpose " "
                    (map (fn [{:keys [text actions]}]
                           (if actions
                             (Button {:actions (:go actions) :content text})
                             (d/strong {} text)))
                         path))))

(q/defcomponent Tab [{:keys [text active? actions]}]
  [:span {:style (merge {:padding "3px 12px 4px"
                         :display "inline-block"}
                        (when active?
                          {:borderBottom "2px solid #2376ef"
                           :marginBottom "-1px"
                           :paddingBottom "3px"}))
          :className (when actions "tab-clickable")}
   text])

(q/defcomponent Header
  :keyfn #(str (-> % :path first :text) "-header")
  [{:keys [path actions]}]
  [:div
   [:div {:style {:background "#f3f3f3"
                  :borderBottom "1px solid #ccc"
                  :display "flex"
                  :justifyContent "space-between"}}
    (map Tab [{:text "Browse" :active? true}])]
   [:div {:style {:display "flex"
                  :justifyContent "space-between"
                  :alignItems "center"
                  :borderBottom "1px solid #ccc"}}
    [DataPath path]
    [CopyButton (:copy actions)]]])

(q/defcomponent Browser
  :keyfn :key
  [{:keys [meta data]}]
  (apply d/tbody {} (map Entry data)))

(def component-map
  {:gadget/browser Browser
   :gadget/string String
   :gadget/number Number
   :gadget/keyword KeywordValue
   :gadget/boolean Boolean
   :gadget/literal Literal
   :gadget/inline-coll InlineCollection
   :gadget/link Link
   :gadget/date Date
   :gadget/code code})

(q/defcomponent DataInspector [data]
  [:div {}
   (Header data)
   [:table {:style {:borderCollapse "collapse"
                    :width "100%"
                    :borderBottom "1px solid #ccc"}}
    (w/postwalk #(get component-map % %) (:hiccup data))]])

(q/defcomponent Inspector [{:keys [data]}]
  [:div {:className "inspector"
         :style {:fontSize "12px"
                 :fontFamily "helvetica neue, lucida grande, sans-serif"
                 :lineHeight "1.5"
                 :color "#333"
                 :textShadow "0 1px 0 rgba(255, 255, 255, 0.6)"}}
   (map DataInspector data)])
