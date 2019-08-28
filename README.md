# ClojureScript Data Browser

Developer tooling for inspecting ClojureScript data structures from running
programs. Inspect data in devtools using the Chrome extension, or in any other
browser using the remote inspector.

## WARNING

This is software in the making, not even alpha level. It *will* have breaking
changes. If you're curious, please see below for usage. If you can wait, this
tool will at some point become stable, at which point breaking changes will
never occur intentionally.

## Inspecting a page

You need the small agent library to expose data for inspection. It is available
from Clojars:

```sh
cjohansen/gadget-inspector {:mvn/version "0.2019.05.20"}
```

Then, either create your app-wide atom with the inspector, or inspect an
existing atom:

```clj
(require '[gadget.inspector :as inspector])

;; Either
(def store (inspector/create-atom "App state" {}))

;; ...or
(def my-store (atom {}))
(inspector/inspect "App state" my-store)
```

You can also inspect regular persistent data structures, for example the data
going into a UI component, with the `inspect` function:

```clj
(def data {:band "Rush"})

(inspector/inspect "Band data" data)
```

The label operates as both an identifier for your data, and a header, so you can
tell structures apart. Inspecting another piece of data with the same label will
replace the corresponding section in the inspector.

### Controlling the liveness of the inspector

By default, the inspector will only update its UI 250ms after changes to your
data occurred. This should help avoid the inspector detracting from your app's
performance. You can override this value if you wish:

```clj
(require '[gadget.inspector :as inspector])

;; Render less frequently
(inspector/set-render-debounce-ms! 1000)

;; Render synchronously on every change
(inspector/set-render-debounce-ms! 0)
```

### Temporarily pausing inspection

Even with debouncing, you may find that the work done by the inspector is too
much in performance critical moments. While this will never be a problem for
regular users in production, it can still be annoying during development. For
those cases, you can pause the inspector, and resume it when the intensive work
has completed:

```clj
(require '[gadget.inspector :as inspector]
         '[cljs.core.async :refer [go <!]))

(inspector/pause!)

(go
  (<! (animate-smoothly))
  (inspector/resume!))
```

The inspector will render immediately when you call `resume!`.

### Inspection filter

Instead of imperatively instructing Gadget to pause and resume all inspections,
you can supply a filter that indicates whether or not data should be passed on
for inspection. This feature is most useful when you inspect atoms, because
those are typically watched and automatically inspected by Gadget. The filter
will receive the data to inspect as its only argument (dereferenced, if it is an
atom), and should return `true` to indicate that inspection is desirable:

```clj
(require '[gadget.inspector :as inspector])

(inspector/inspect
  "App store"
  (atom {})
  {:inspectable? (fn [state] (not (:page-transition state)))})
```

Filters also work for non-atom data, which can be useful if you already apply a
filter to your inspected atoms, and would like to reuse the logic for one-off
inspections:

```clj
(require '[gadget.inspector :as inspector])

(def inspection-opts
  {:inspectable? (fn [state] (not (:page-transition state)))})

(inspector/inspect "App store" (atom {}) inspection-opts)

(defn render-app [component page-data element]
  (inspector/inspect "Page data" page-data inspection-opts)
  (render (component page-data) element))
```

## Using the Chrome extension

Start by building the extension. Requires the `clojure` binary:

```sh
make extension
```

To load up the Chrome extension, open
[chrome://extensions/](chrome://extensions/), toggle the "Developer mode" toggle
in the upper right corner, and then click "Load unpacked" in the upper left
corner. Select the `extension` directory in this repository.

Load your page, open devtools, and then click on the `CLJS Data` panel. Rejoice.
*NB!* If you've had devtools open since installing the extension, you might need
to toggle it once.

## Using the remote inspector

To inspect remotely you'll need to run a small web server. To do this you need
Go (eventually we'll ship compiled binaries):

```sh
make remote-server
```

Now open an inspected page in any browser without the extension, and then open
[http://localhost:7117](http://localhost:7117). It should display a list of
app/browser combinations for you to inspect. Click one, and bask in the data
glory presented to you.

## Teaching Gadget new tricks

Gadget assigns every piece of data a keyword "type" that it uses to dispatch a
number of multi-methods to build its UI. By adding new type inferences, you can
teach Gadget to use custom rendering and navigation for certain kinds of data -
e.g. render "person maps" differently from other maps, render custom literal
values in your app, etc.

You can override Gadget's multi-methods for built-in types such as `:string` as
well if you want to change how they are rendered.

### Type inferences

New type inferences are added with `gadget.core/add-type-inference`:

```clj
(require '[gadget.core :as gadget])

(gadget/add-type-inference
  (fn [data]
    (when (:person/id data)
      :person)))
```

The type inference function is passed a single argument - a piece of data. It
should return either a keyword indicating the type of data, or `nil` to indicate
that it does not recognize the type of the data.

When Gadget is identifying the type of some data, it will call these functions
starting with the last one added. As soon as it finds a non-nil result, it will
use that and stop calling inference functions.

### Getting and navigating data

Gadget provides a function for converting values to browsable data and navigate
them, just like
[`clojure.datafy`](https://clojure.github.io/clojure/branch-master/clojure.datafy-api.html).

`gadget.core/datafy` is a multimethod that converts a value to data. This
multi-method allows Gadget to differentiate values based on the synthetic type
describe above. While this would be possible through [procotol extension by
metadata](https://github.com/clojure/clojure/blob/master/changes.md#22-protocol-extension-by-metadata),
that would require upfront processing of data to inspect, and would interfere
with the plug and play goal of Gadget. Instead, this light abstraction on top of
`datafy` makes it easy to plug in new "types" that increases insight and
understand when peering through your data: browsing the claims of a JWT token,
or the year, month, day, and other fields of a timestamp. The default
implementation of `gadget.core/datafy` is to call `clojure.datafy/datafy`.

To navigate data, Gadget uses `clojure.datafy/nav` directly. Because `datafy`
can associate data with protocols using metadata, there is no need for a
dedicated `nav` implementation in Gadget - just make sure that the return value
from `datafy` implements the `Navigable` protocol via metadata, if needed.

Gadget passes both the raw and datafyed value to the renderers, so the renderers
can use either/or in the visualization. The datafyed version is always used to
find navigable keys/indices.

As an example, let's consider how Gadget recognizes and navigates JWTs:

```clj
(require '[clojure.string :as str]
         '[gadget.core :as gadget])

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(gadget/add-type-inference
  (fn [data]
    (when (and (string? data) (re-find re-jwt data))
      :jwt)))

(defn- base64json [s]
  (-> s js/atob JSON.parse (js->clj :keywordize-keys true)))

(defmethod gadget/datafy :jwt [token]
  (let [[header data sig] (str/split token #"\.")]
    {:header (base64json header)
     :data (base64json data)
     :signature sig}))

(defmethod gadget/render [:inline :jwt] [_ {:keys [raw]}]
  [:strong "JWT: " [:gadget/string raw]])
```

Let's see this in practice. Given this data:

```clj
(def data
  {:token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"})
```

When Gadget renders this map, the token will be rendered with the `:inline`
view. This view is passed both the token string, and the map returned from the
`datafy` implementation. This particular inline view renders the token string
with the `"JWT: "` prefix. Since the datafy result is a map, the token will be
navigable in the browser.

When you click the token, Gadget will render the token in the `:full` view. For
`:jwt`, this view renders the datafyed value with the map browser. Since this is
a map, it can be navigated deeper without any further ado.

### Rendering custom data types

The first step in rendering custom data types is to define a custom type
inference. Let's say we have some literals from the server that we want to
preserve with their literal form, such as
[java.time literals](https://github.com/magnars/java-time-literals).

Step 1 is to define the type inference:

```clj
(require '[gadget.core :as gadget])

(gadget/add-type-inference
  (fn [data]
    (when (and (string? data) (re-find #"^#time" data))
      :java-time-literal)))
```

The Java time literals are just strings on the client - if we do nothing else,
they will be rendered as strings, e.g. `"#time/ld \"2019-08-01\""`. To make them
render as literals, we'll implement the renderer function for inline views of
the `:java-time-literal` type:

```cl
(require '[clojure.string :as str]
         '[gadget.core :as gadget])

(defmethod gadget/render [:inline :java-time-literal] [_ {:keys [raw]}]
  (let [[prefix str] (str/split raw " ")]
    [:gadget/literal {:prefix prefix
                      :value (cljs.reader/read-string str)}]))
```

There is no point in implementing the `:full` view, because the literal is a
string, and strings are not navigable by default, thus it will never be rendered
with the `:full` view.

We could implement `datafy` for the literal and produce a JS date object, and
then use that to power a `:full` view:

```clj
(require '[clojure.string :as str]
         '[gadget.core :as gadget])

(defmethod gadget/datafy :java-time-literal [literal]
  (let [[prefix str] (str/split raw " ")]
    (case prefix
      "#time/ld" (let [date-str (cljs.reader/read-string str)
                       [y m d] (map #(js/parseInt % 10) (str/split date-str
                       #"-"))]
                   (js/Date. y (dec m) d)))))
```

Gadget's built-in instant tooling would pick this up and allow you to navigate
into this value to see year, month, date, etc. Please disregard the abomination
that is converting a local date to an instant like this.

## Components

Gadget allows renderers to return pure unadulterated
[hiccup](https://github.com/weavejester/hiccup), essentially allowing you to
dictate the exact rendering of a piece of data. To avoid re-implementing
components that the Gadget inspector already uses in an uncanny valley
all-you-can-eat, Gadget allows you to use some components via namespaced
keywords.

### Basic types

Dedicated inline components for basic types. All of these take the value as
their only argument, e.g:

```clj
;; Just the string component
[:gadget/string "Some string"]

;; Embedded in other markup
[:div {}
  [:gadget/string "Some string"]
  [:gadget/number 12]]
```

The available types are:

- `:gadget/string`
- `:gadget/number`
- `:gadget/keyword`
- `:gadget/map-keys`
- `:gadget/boolean`
- `:gadget/inst`

### Code

Inline code samples can be created with `[:gadget/code code-string]`.

### Map browser

Most `:full` renders use the browser component. It takes a sequence of key-value
pairs:

```clj
[:gadget/browser kv-coll]
```

The keys should be some inline markup, while the values should be maps of
`:actions` and `:hiccup`.

## Reference

All functions that are exposed for extension.

### `(defmethod gadget.core/copy type-kw [v])`

Returns the value copied to the copy buffer when clicking the copy button next
to this value. Dispatches on the synthetic keyword type. The default
implementation returns `(pr-str v)`.

### `(defmethod gadget.core/render [view type] [view data & [view-opts]])`

Render the data of type `type` with view `view`. Currently `view` is one of
`:inline` or `:full`. `data` is a map with keys `:raw` - the raw data, `:type` -
the synthetic type, and `:data` - the datafied data.

### `(gadget.core/add-type-inference type [f])`

Add type inference. Type inference is done by calling each registered function
until a non-nil value is returned. LIFO - the last registered inference will be
tried first. The function should return `nil` for unknown values, otherwise a
keyword that names the "type". Any other return value will cause an error.

### `(gadget.core/datafy type [val])`

A wrapper around `clojure.datafy/datafy` that dispatches on Gadget's synthtic
types. The default implementation calls `clojure.datafy/datafy`.
