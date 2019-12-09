/*global browser, chrome*/

const script = document.createElement("script");
const ua = typeof chrome !== 'undefined' ? chrome : browser;

script.textContent = `window.cljs_data_browser = message => {
  window.postMessage({
    id: "cljs-data-browser",
    message: message
  }, "*");
};
`;

script.onload = () => script.parentNode.removeChild(script);
(document.head || document.documentElement).appendChild(script);

window.addEventListener('message', event => {
  if (event.data.id === "cljs-data-browser") {
    try {
      ua.runtime.sendMessage(event.data);
    } catch (e) {
      console.warn("Failed to relay message", e);
    }
  }
});

ua.runtime.onMessage.addListener(msg => {
  window.postMessage({
    id: "cljs-data-browser-action",
    message: msg.message
  }, "*");
});

console.log("Plugin gadget-browser initialized successfully")
