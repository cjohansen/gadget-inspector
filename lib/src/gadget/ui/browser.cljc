(ns gadget.ui.browser
  (:require [gadget.ui.elements :as e]))

(defn entry [{:keys [k v onClick button]}]
  [(if onClick :tr.clickable :tr) {:onClick onClick}
   [:th k]
   [:td [:span.flex v (some-> button e/button)]]])

(defn table [{:keys [entries]}]
  [:table.table
   [:tbody {} (map entry entries)]])

(defn path [{:keys [entries]}]
  [:ul.hl
   (for [entry entries]
     [:li
      [(if (:actions entry)
         :a.pill.pill-big
         :span.pill.pill-ph)
       (if-let [text (:text entry)]
         [:code text]
         (e/value entry))]])])

(defn browser [data]
  [:div
   [:div.flex.navbar
    (path (:path data))
    (when-let [button (:button data)]
      (e/button button))]
   (table data)])

(defn toolbar [data]
  [:div.toolbar
   (for [tab (:tabs data)]
     [:a
      {:className (str "tab" (when (:selected? tab)
                               " tab-selected "))
       :onClick (:actions tab)}
      (:text tab)])])

(defn panel [data]
  [:div.panel
   (toolbar data)
   (browser data)])
