(ns dev.core
  (:require [funzt.tabinski :as tab]
            [minreact.core :as m :refer-macros [defreact]]
            [sablono.core :refer-macros [html]]
            cljsjs.react))

(defn border-div [& children]
  (html
    [:div {:style {:border "solid 1px"}} children]))

(defreact self-coloring-input [inst tab-key n]
  :state {:keys [focused?]}
  (fn render []
    (tab/tab
     {:tab-id tab-key}
     (html
       [:input {:value n
                :style {:background-color
                        (if focused?
                          "green")}
                :on-focus
                (fn [e]
                  (m/set-state! this
                                :focused? true))
                :on-blur
                (fn [e]
                  (m/set-state! this
                                :focused? false))}]))))

(defreact main-ui [inst]
  (fn render []
    (tab/tab-group
     {:order [2 1 3]
      :dom-elem js/document}
     (m/with-irefs [s tab/tabinski-state]
       (html
         [:div {:style {:color "green"}} "Hello"
          [:div (pr-str s)]
          (for [x (range 1 4)]
            (tab/tab-group
             {:tab-id x
              :order [2 1 3]}
             (html
               [:div
                (for [y (range 1 4)]
                  (self-coloring-input inst y (* x y)))])))])))))

(js/React.render (main-ui {})
                 (js/document.getElementById "app"))
