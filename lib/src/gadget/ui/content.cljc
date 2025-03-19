(ns gadget.ui.content)

(defn article [{:keys [title sections]}]
  [:div
   [:h1.h1 title]
   (for [{:keys [text code title image]} sections]
     [:div
      (when title
        [:h2.h2 title])
      (when text
        [:p.p text])
      (when code
        [:p [:pre.code code]])
      (when image
        [:p.center
         [:img image]])])])
