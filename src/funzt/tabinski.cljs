(ns funzt.tabinski
  "Tab-friendliness in React based applications"
  (:require [minreact.core :refer-macros [defreact] :as m]
            [goog.events :as events]
            [goog.dom :as dom]
            [clojure.set :refer [rename-keys]]
            [cljsjs.react.dom])
  (:import [goog.events KeyHandler EventType]))

(defn- parents-seq
  "Return a lazy sequence of the parents of dom-elem"
  [dom-elem]
  (->> dom-elem
       (iterate (fn [elem]
                  (.-parentNode elem)))
       (take-while some?)))

(defn- tabinski-parents
  "Get tabinski parent groups of dom-elem"
  [tabinski-elems dom-elem]
  (->> (parents-seq dom-elem)
       (filter #(= :group
                   (:type (get-in tabinski-elems [%]))))))

(def ^:private get-tabinski-parent
  (comp first tabinski-parents))

(def ^:private get-tabinski-root
  (comp last tabinski-parents))

(defn- direct-tabinski-children
  "Direct tabinski children in the order they should be tabbed.  These
  are direct and indirect DOM children of elem, but none of them are
  children of each other."
  [tabinski-elems elem]
  (if (.hasChildNodes elem)
    (let [parent (get-tabinski-parent tabinski-elems elem)
          order (:group/order (get tabinski-elems parent))]
      (cond->> (mapcat (fn [elem]
                         (if (contains? tabinski-elems elem)
                           [elem]
                           (direct-tabinski-children tabinski-elems elem)))
                       (array-seq (.-childNodes elem)))
        order
        (sort (fn [elem1 elem2]
                (let [id1 (get-in tabinski-elems [elem1 :key])
                      id2 (get-in tabinski-elems [elem2 :key])]
                  (let [r (filter #{id1 id2} order)]
                    (cond (= [id1 id2] r)
                          -1
                          (= [id2 id1] r)
                          1
                          :else
                          0)))))))))

(defn- all-tabinski-children
  "Lazy sequence with the result of a deep recursive search for
  elements that are tabinski in the order they should be tabbed.  See
  direct-tabinski-children."
  [tabinski-elems elem]
  (mapcat (fn [tabinski-elem]
            (cons tabinski-elem
                  (all-tabinski-children tabinski-elems
                                         tabinski-elem)))
          (direct-tabinski-children tabinski-elems elem)))

(defonce tabinski-state
  #_"{:type (:group | :tab | :break)
   :group/listener event-listener
   :group/order [key ...]
   :key any-value}"
  (atom {}))

(defn focusable-dom-elem
  "If dom-elem is focusable, returns dom-elem, otherwise the direct or
  indirect child that is focusable."
  [dom-elem]
  (if (dom/isFocusable dom-elem)
    dom-elem
    (dom/findNode dom-elem
                  #(if (dom/isElement %)
                     (dom/isFocusable %)))))

(defn exec-tab
  "Trigger tabinski effect of tab key press"
  [backwards?]
  (let [{:keys [current-elem tabinski-elems]}
        (swap! tabinski-state
               (fn [{:keys [current-elem tabinski-elems]
                     :as state}]
                 (let [current-elem
                       (let [active (dom/getActiveElement js/document)]
                         (first (filter #(contains? tabinski-elems %)
                                        (parents-seq active))))
                       sorted-elems
                       (->> (cond->
                                (all-tabinski-children
                                 tabinski-elems
                                 (or (get-tabinski-root tabinski-elems
                                                        current-elem)
                                     js/document))
                              backwards? (reverse))
                            (remove (fn [elem]
                                      (= (get-in tabinski-elems
                                                 [elem :type])
                                         :group))))]
                   (assoc state
                     :current-elem
                     (if (some #{current-elem} sorted-elems)
                       (->> sorted-elems
                            (cycle)
                            (drop-while
                             (complement #{current-elem}))
                            (second))
                       (first sorted-elems))))))]
    (if-let [do-focus (get-in tabinski-elems [current-elem :do-focus])]
      (do-focus current-elem)
      (.focus (focusable-dom-elem current-elem)))))

(defreact tab-group
  "Wrap child in a tab group.  The following opts are supported:

  :tab-id - local identifier that can be used in a parent
            tab-groups :order

  :order - order of tab-groups or tabs within this group

  :dom-elem - Experimental.  A dom element on which the keyboard
  listener will be installed.  Use this for a known root tab-group in
  an application to have the listener e. g. on js/document."
  [{:keys [dom-elem] :as opts} child]
  (fn componentDidMount []
    (let [dom-elem (or dom-elem
                       (js/ReactDOM.findDOMNode this))]
      ;; TODO: prevent this from throwing in direct-tabinski-children
      ;; when dom-elem is nil
      
      ;; Make this this dom element trees master node by stopping all
      ;; group listeners on children
      (run! (fn [dom-elem]
              (when-let [listener-function
                         (get-in @tabinski-state
                                 [:tabinski-elems dom-elem :group/listener])]
                (events/unlisten dom-elem
                                 EventType.KEYDOWN
                                 listener-function)
                (swap! tabinski-state
                       update-in [:tabinski-elems dom-elem]
                       dissoc :group/listener)))
            (direct-tabinski-children (:tabinski-elems @tabinski-state)
                                      dom-elem))
      (swap! tabinski-state assoc-in
             [:tabinski-elems dom-elem]
             (-> opts
                 (assoc :type :group)
                 (rename-keys {:order :group/order
                               :tab-id :key})))
      (when (= (get-tabinski-root (:tabinski-elems @tabinski-state)
                                  dom-elem)
               dom-elem)
        (let [listener-function (fn [e]
                                  (when (== 9 (.-keyCode e))
                                    (.preventDefault e)
                                    (exec-tab (.-shiftKey e))))]
          (swap! tabinski-state assoc-in
                 [:tabinski-elems dom-elem :group/listener]
                 listener-function)
          (events/listen dom-elem
                         EventType.KEYDOWN
                         listener-function)))))
  (fn componentWillReceiveProps [[next-opts]]
    (assert (= (:dom-elem opts)
               (:dom-elem next-opts))
            "Can't change global listener dom-elem dynamically")
    (when-not (= (select-keys next-opts [:tab-id :order])
                 (select-keys opts [:tab-id :order]))
      (swap! tabinski-state update-in
             [:tabinski-elems (or dom-elem
                                  (js/ReactDOM.findDOMNode this))]
             merge
             (-> next-opts
                 (rename-keys {:order :group/order
                               :tab-id :key})))))
  (fn componentWillUnmount []
    (let [cdom-node (js/ReactDOM.findDOMNode this)]
      (when dom-elem
        (events/unlisten dom-elem
                         EventType.KEYDOWN
                         (get-in @tabinski-state
                                 [:tabinski-elems dom-elem :group/listener])))
      (swap! tabinski-state update :tabinski-elems dissoc cdom-node)))
  (fn render [] child))

(defn- add-dom-elem! [elem {:keys [tab-id
                                   do-focus]}]
  (swap! tabinski-state
         assoc-in
         [:tabinski-elems elem]
         {:type :tab
          :key tab-id
          :do-focus do-focus}))

(defn- remove-dom-elem! [elem]
  (swap! tabinski-state
         update
         :tabinski-elems
         dissoc elem))

(defreact tab
  "Must wrap a child that can be focused.  The following opts are
  supported:

  :tab-id - local identifier that can be used in a parent
            tab-groups :order

  :do-focus - If provided a function invoked with the child DOM node
              implementing custom logic to run on tab key press"
  [opts child]
  :state {:keys [dom-elem]}
  (fn componentDidMount []
    (let [dom-elem (js/ReactDOM.findDOMNode this)]
      (m/set-state! this :dom-elem dom-elem)
      (add-dom-elem! dom-elem opts)))
  (fn componentWillReceiveProps [[next-opts]]
    (when-not (= (select-keys next-opts
                              [:tab-id
                               :do-focus])
                 (select-keys opts
                              [:tab-id
                               :do-focus]))
      (add-dom-elem! dom-elem next-opts)))
  (fn componentDidUpdate []
    (let [new-dom-elem (js/ReactDOM.findDOMNode this)]
      (when-not (= new-dom-elem dom-elem)
        (m/set-state! this :dom-elem new-dom-elem)
        (remove-dom-elem! dom-elem)
        (add-dom-elem! new-dom-elem opts))))
  (fn componentWillUnmount []
    (when (= (:current-elem @tabinski-state)
             dom-elem)
      (exec-tab false))
    (remove-dom-elem! dom-elem))
  (fn render [] child))
