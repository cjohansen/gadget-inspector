/*global browser, chrome*/

const script = document.createElement("script");
const ua = typeof chrome !== 'undefined' ? chrome : browser;

/**
 * This script is injected into every page opened when the extension is
 * installed. It will be evaluated in the context of the page, and the code does
 * NOT have access to the local scope in this script file.
 */
script.textContent = `window.cljs_data_browser = message => {
  window.postMessage({
    id: "cljs-data-browser",
    message: message
  }, "*");
};
`;

script.onload = () => script.parentNode.removeChild(script);
(document.head || document.documentElement).appendChild(script);

/**
 * This listener relays the message from the above injected script. THIS piece
 * of code DOES have access to the local scope in this file, and thus can reach
 * `ua.runtime`.
 */
window.addEventListener('message', event => {
  if (event.data.id === "cljs-data-browser") {
    try {
      ua.runtime.sendMessage(event.data);
    } catch (e) {
      console.log("Failed to relay message", e);
    }
  }
});

/**
 * This listener receives messages from the extension panel (via the background
 * page), and relays it to the gadget library, which runs a listener for
 * messages on `window`.
 */
ua.runtime.onMessage.addListener(msg => {
  window.postMessage({
    id: "cljs-data-browser-action",
    message: msg
  }, "*");
});

console.log("Plugin gadget-browser initialized successfully")
