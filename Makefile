inspector/target/panel.js: inspector/src/**/*
	cd inspector && clojure -A:build-panel

extension/panel.js: inspector/target/panel.js
	cp inspector/target/panel.js extension/panel.js

extension/inspector.css: lib/resources/public/inspector.css
	cp lib/resources/public/inspector.css extension/inspector.css

extension: extension/panel.js extension/inspector.css
