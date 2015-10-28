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
    (m/with-irefs [s tab/tabinski-state]
      (tab/tab-group
       {:order [2 1 3]
        :dom-elem js/document}
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

(defreact readme-example [inst]
  :state {:keys [group-3-order]}
  (fn getInitialState []
    {:group-3-order [2 1 3]})
  (fn render []
    (tab/tab-group
     { ;; the order in which we want the contained child groups and
      ;; tabs entered:
      :order [:single-tab 
              :group-1
              :group-2
              :group3]
      ;; (the outmost tab-group of your app can optionally specify a
      ;; custom dom element to install the keyboard listener on:)
      :dom-elem js/document}
     (html
       [:div
        (tab/tab {:tab-id :single-tab}  ; via :tab-id the containing
                                        ; tab-group can identify this tab
                 (html [:input {:value "single-tab"}]))
        (tab/tab-group
         {:tab-id :group-1
          :order [3 2 1]}
         (html
           [:div
            (tab/tab
             {:tab-id 1}     ; :tab-ids must only be unique within the
                                        ; containing tab-group
             (html [:input {:value "Input group-1/1"}]))
            (tab/tab
             {:tab-id 2}
             (html [:input {:value "Input group-1/2"}]))
            (tab/tab
             {:tab-id 3}
             (html [:input {:value "Input group-1/3"}]))]))
        (tab/tab-group
         {:tab-id :group-2}             ; the :order is optional
         (html
           [:div
            (for [n (range 1 4)]
              (tab/tab
               {}                       ; ... so is the :tab-id (the
                                        ; dom order will be used then
                                        ; which resembles native
                                        ; behavior)
               (html [:input {:value (str "Input group-2/" n)}])))]))
        (tab/tab-group
         {:tab-id :group-3
          :order group-3-order ; like the groups children, the tab
                               ; order can be specified dynamically
          }
         (html
           [:div
            (for [n (range 1 4)]
              (tab/tab
               {:tab-id n}
               (html [:input {:value (str "Input group-3/" n)}])))]))
        [:div (str "group-3-order: " group-3-order)]
        [:button {:on-click (fn [_]
                              (m/state! this update :group-3-order shuffle))}
         "Shuffle group-3-order"]]))))

(js/React.render (readme-example {})
                 (js/document.getElementById "app"))
