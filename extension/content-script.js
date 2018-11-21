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

window.addEventListener('message', function(event) {
  chrome.runtime.sendMessage(event.data);
});
