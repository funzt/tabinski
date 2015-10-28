# tabinski

`[org.funzt/tabinski "0.1.0"]` is a ClojureScript library enabling programmers to specify tab order in React based applications declaratively and dynamically in composable fashion.

# Why?

The `tabIndex` attribute is not helpful in component based applications because it needs to be specified by an instance with global responsibility.  What users really want is to specify the tab order locally within a component.

# How?

Tabinski uses a keyboard event listener to intercept tab keypresses and focuses the next element according to the order you have specified.  Here is an annotated example program which should get you going.  It uses the [minreact](https://github.com/lgrapenthin/minreact) react adapter but should work fine with any other (like om, reagent).

```clojure
(defreact readme-example []
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
         
(js/React.render (readme-example) (js/document.getElementById "app"))         
```

## Caveats

- Within a `tab-group` it is not possible to get the native browser behavior.  If you want something to be focusable via tab, you have to wrap it within `tabinski/tab`.  If you need an escape component, please raise an issue or implement it :)

- `tabinski` is easily mistyped as `tabinksi`.  This problem goes away after a while of using it :)

## Feedback

tabinski is in an early state.  It is likely that you have custom needs not yet met by tabinski.  Please raise an issue.

## License

Copyright © 2015 Leon Grapenthin and [brevido.com](http://brevido.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
