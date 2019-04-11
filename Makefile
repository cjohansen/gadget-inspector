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
	cd inspector && clojure -A:build-remote-inspector

remote/static/assets/remote-inspector.js: inspector/target/remote-inspector.js remote/static/assets
	cp inspector/target/remote-inspector.js remote/static/assets/remote-inspector.js

remote/static/assets/remote-inspector.js.map: inspector/target/remote-inspector.js.map remote/static/assets
	cp inspector/target/remote-inspector.js.map remote/static/assets/remote-inspector.js.map

remote/static/assets/inspector.css: lib/resources/public/inspector.css remote/static/assets
	cp lib/resources/public/inspector.css remote/static/assets/inspector.css

remote-inspector: remote/static/assets/remote-inspector.js remote/static/assets/remote-inspector.js.map remote/static/assets/inspector.css

remote-server: remote/static/assets/remote-inspector.js remote/static/assets/remote-inspector.js.map remote/static/assets/inspector.css
	cd remote && go run server.go

lib/target:
	mkdir target

lib/target/gadget-inspector.jar: lib/target lib/src/**/*.*
	cd lib && clojure -A:jar

deploy: lib/target/gadget-inspector.jar
	cd lib && mvn deploy:deploy-file -Dfile=target/gadget-inspector.jar -DrepositoryId=clojars -Durl=https://clojars.org/repo -DpomFile=pom.xml

clean:
	rm -fr remote/static/assets inspector/target extension/panel.js extension/panel.js.map extension/inspector.css lib/target

.PHONY: remote-inspector remote-server deploy clean
