inspector/target/panel.js: inspector/src/**/*
	cd inspector && clojure -A:build-panel

extension/panel.js: inspector/target/panel.js
	cp inspector/target/panel.js extension/panel.js

extension: extension/panel.js
