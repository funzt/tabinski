(ns funzt.tabinski
  "Tab-friendliness in React based applications"
  (:require [cljsjs.react]
            [goog.events :as events]
            [goog.dom :as dom])
  (:import [goog.events KeyHandler EventType]))

(defn tab
  "Return a react ref registering the associated DOM object.  Opts is
  a map with the following keys

  :key - A key to refer to this tab-nav in a containing tab-nav

  :order - Keys of tab navs that are direct or indirect dom children
  of this tab nav (but not other tab navs).  The order in which they
  should be tabbed.  Specify :auto to use the hierarchical dom order."
  [tabinski opts]
  (let [this-dom-elem (atom nil)]
    (fn [react-component]
      (if react-component
        (let [dom-elem (.getDOMNode react-component)]
          (reset! this-dom-elem dom-elem)
          (swap! (:state tabinski)
                 (fn [state]
                   (-> state
                       (assoc-in [:by-elem dom-elem] opts)))))
        (let [dom-elem @this-dom-elem]
          (swap! (:state tabinski)
                 (fn [state]
                   (-> state
                       (update :by-elem dissoc dom-elem)))))))))

(defn- parents-seq
  "Return a lazy sequence of the parents of dom-elem"
  [dom-elem]
  (->> dom-elem
       (iterate (fn [elem]
                  (.-parentNode elem)))
       (take-while some?)))

(defn- get-tabinski-parent
  "Get next parent that has tabinski information"
  [{:keys [by-elem] :as tabinski-state} dom-elem]
  (->> (parents-seq dom-elem)
       (filter #(contains? by-elem %))
       first))

(defn- get-key [identifier]
  (if (keyword? identifier)
    identifier
    (:key identifier)))

(defn- direct-tabinski-children
  "Direct tabinski children according to tabinski definition"
  [{:keys [by-elem] :as tabinski-state} elem]
  (if (.hasChildNodes elem)
    (let [parent (get-tabinski-parent tabinski-state elem)
          order (:order (get by-elem parent))]
      (cond->> (mapcat (fn [elem]
                         (if (contains? by-elem elem)
                           [elem]
                           (direct-tabinski-children tabinski-state elem)))
                       (array-seq (.-childNodes elem)))
        (and order
             (not= :auto order))
        (sort (fn [elem1 elem2]
                (let [id1 (get-key (get by-elem elem1))
                      id2 (get-key (get by-elem elem2))]
                  (let [r (filter #{id1 id2} order)]
                    (cond (= [id1 id2] r)
                          -1
                          (= [id2 id1] r)
                          1
                          :else
                          0)))))))))

(defn- ordered-tabinski-children
  "Lazy sequence with the result of a deep recursive search for
  elements that are tabinski in the order they should be tabbed."
  [{:keys [by-elem] :as tabinski-state} elem]
  (mapcat (fn [tabinski-elem]
            (cons tabinski-elem
                  (ordered-tabinski-children tabinski-state
                                             tabinski-elem)))
          (direct-tabinski-children tabinski-state elem)))

(defn exec-tab
  "Trigger tabinski effect of tab key press"
  [{:keys [state get-next-ids] :as tabinski} backwards?]
  (let [{:keys [current-elem by-elem]}
        (swap! state
               (fn [{:keys [current-elem by-elem]
                     :as state}]
                 (let [sorted-elems
                       (->> (cond-> (ordered-tabinski-children state
                                                               js/document)
                              backwards? (reverse))
                            (remove (fn [elem]
                                      (contains? (get by-elem elem)
                                                 :order))))]
                   (assoc state
                     :current-elem
                     (if (some #{current-elem} sorted-elems)
                       (->> sorted-elems
                            (cycle)
                            (drop-while
                             (complement #{current-elem}))
                            (second))
                       (first sorted-elems))))))]
    (.focus current-elem)))

(defn start
  "Create an instance of tabinski, return it.  Prevents default action
  of TAB on js/document."
  [{:keys [get-next-ids] :as config}]
  (let [state (atom {:current-id nil})
        this {:config config
              :get-next-ids get-next-ids
              :state state}
        listener-function
        (fn [e]
          (when (== 9 (.-keyCode e))
            (.preventDefault e)
            (exec-tab this (.-shiftKey e))))]
    (events/listen js/document
                   EventType.KEYDOWN
                   listener-function)
    (assoc this ::listener-function listener-function)))

(defn stop
  "Stop tabinski instance (return value of start)."
  [{:keys [::listener-function] :as inst}]
  (events/unlisten js/document
                   EventType.KEYDOWN
                   listener-function)
  inst)

