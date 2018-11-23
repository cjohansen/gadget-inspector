/*global chrome*/

const script = document.createElement("script");

script.textContent = `window.cljs_atom_browser = message => {
  window.postMessage({
    id: "cljs-atom-browser",
    message: message
  }, "*");
};
`;

script.onload = () => script.parentNode.removeChild(script);
(document.head || document.documentElement).appendChild(script);

window.addEventListener('message', event => {
  if (event.data.id === "cljs-atom-browser") {
    chrome.runtime.sendMessage(event.data);
  }
});

chrome.extension.onMessage.addListener((msg, sender, sendResponse) => {
  window.postMessage({
    id: "cljs-atom-browser-action",
    message: msg
  }, "*");
});

