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
replace the corresponding section in the extension.

### Temporarily pausing inspection

In performance critical moments, continuously serializing data for the inspector
may detract from the user experience. While this will never happen to regular
users in production, it can still be annoying during development. For those
cases, you can pause the inspector, and resume it when the intensive work has
completed:

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
