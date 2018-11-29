# ClojureScript Data Browser

Developer tooling for inspecting ClojureScript data structures from running
programs. Inspect data in devtools using the Chrome extension, or in any other
browser using the remote inspector.

## Inspecting a page

You need the small agent library to expose data for inspection. For development,
here's a quick cheat:

```sh
cd path/to/your/app/src
ln -s /Users/yourname/projects/cljs-data-browser/lb/src/gadget
```

Then, replace your app-wide atom with:

```clj
(require '[gadget.inspector :as inspector])

(inspector/create-atom "App state" {})
```

## Using the Chrome extension

Start by building the extension. Requires the `clojure` binary:

```sh
make extensions
```

To load up the Chrome extension, open
[chrome://extensions/](chrome://extensions/), toggle the "Developer mode" toggle
in the upper right corner, and then click "Load unpacked" in the upper left
corner. Select the `extension` directory in this repository.

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
