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
cjohansen/gadget-inspector {:mvn/version "0.2019.04.16"}
```

Then, wither create your app-wide atom with the inspector, or inspect an
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
