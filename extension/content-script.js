/*global chrome*/

const script = document.createElement("script");

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
    browser.runtime.sendMessage(event.data);
  }
});

browser.runtime.onMessage.addListener((msg) => {
  window.postMessage({
    id: "cljs-data-browser-action",
    message: msg.message
  }, "*");
});

console.log("Plugin gadget-browser initialized successfully")
