inspector/target/panel.js: inspector/src/**/* inspector/deps.edn inspector/cljs.edn
	cd inspector && clojure -A:build-panel

extension/panel.js: inspector/target/panel.js
	cp inspector/target/panel.js extension/panel.js

extension/panel.js.map: inspector/target/panel.js.map
	cp inspector/target/panel.js.map extension/panel.js.map

extension/inspector.css: lib/resources/public/inspector.css
	cp lib/resources/public/inspector.css extension/inspector.css

extension: extension/panel.js extension/panel.js.map extension/inspector.css

remote/static/assets:
	mkdir -p remote/static/assets

inspector/target/remote-inspector.js: inspector/src/**/* inspector/deps.edn inspector/cljs.edn remote/static/assets
	clojure -A:build-remote-inspector

remote/static/assets/remote-inspector.js: inspector/target/remote-inspector.js remote/static/assets
	cp inspector/target/remote-inspector.js remote/static/assets/remote-inspector.js

remote/static/assets/remote-inspector.js.map: inspector/target/remote-inspector.js.map remote/static/assets
	cp inspector/target/remote-inspector.js.map remote/static/assets/remote-inspector.js.map

remote/static/assets/inspector.css: lib/resources/public/inspector.css remote/static/assets
	cp lib/resources/public/inspector.css remote/static/assets/inspector.css

remote-inspector: remote/static/assets/remote-inspector.js remote/static/assets/remote-inspector.js.map remote/static/assets/inspector.css

remote-server: remote/static/assets/remote-inspector.js remote/static/assets/remote-inspector.js.map remote/static/assets/inspector.css
	cd remote && go run server.go

.PHONY: remote-inspector remote-server
