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
    (html
      [:input {:value n
               :ref (tab/tab (:tabinski inst)
                             {:key tab-key})
               
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
                               :focused? false))}])))

(defreact main-ui [inst]
  (fn render []
    (html
      [:div {:ref (tab/tab (:tabinski inst)
                           {:order [2 1 ]})
             :style {:color "green"}} "Hello"
       (for [x (range 1 4)]
         [:div
          {:ref (tab/tab (:tabinski inst)
                         {:key x
                          :order [2 1 3]})}
          (for [y (range 1 4)]
            (self-coloring-input inst y (* x y)))])])))

(defonce instance nil)

(defn start []
  (let [inst {:tabinski (tab/start {})}]
    (set! instance inst)
    (js/React.render (main-ui inst)
                     (js/document.getElementById "app"))))

(defn stop []
  (tab/stop (:tabinski instance)))

(defn reset []
  (if instance
    (do
      (stop)
      (start))
    (start)))

(reset)
