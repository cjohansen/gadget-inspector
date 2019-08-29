/*global chrome*/

const konsole = chrome.extension.getBackgroundPage().console;
var panelWindow;
var queued;

const debounce = (fn, delay) => {
  var timeout;
  return function () {
    const args = arguments;
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      timeout = null;
      fn.apply(null, args);
    }, delay || 200);
  };
};

const backgroundPageConnection = chrome.runtime.connect({
  name: "devtools-page"
});

function setWindowSize(win) {
  chrome.tabs.query({active: true, currentWindow: true}, tabs => {
    chrome.tabs.sendMessage(
      tabs[0].id,
      `{:action :set-window-size :args [{:width ${win.innerWidth}, :height ${win.innerHeight}}]}`
    );
  });
}

chrome.devtools.panels.create(
  "CLJS Data",
  "",
  "panel.html",
  panel => {
    panel.onShown.addListener(function onShown(panelWin) {
      panel.onShown.removeListener(onShown);
      panelWindow = panelWin;

      setWindowSize(panelWindow);

      panelWindow.onresize = debounce(() => {
        setWindowSize(panelWindow);
      }, 150);

      if (queued) {
        panelWindow.receiveMessage(queued);
        queued = null;
      }
    });
  }
);

backgroundPageConnection.postMessage({
  tabId: chrome.devtools.inspectedWindow.tabId,
  name: "init"
});

chrome.devtools.network.onNavigated.addListener(url => {
  if (panelWindow) {
    panelWindow.receiveMessage({
      request: {
        id: "cljs-data-browser",
        type: "reset"
      }
    });
  } else {
    konsole.log("Unqueue message");
    queued = null;
  }
});

backgroundPageConnection.onMessage.addListener((request, sender, sendResponse) => {
  if (panelWindow) {
    panelWindow.receiveMessage({request, sender});
  } else {
    queued = {request, sender};
  }
});
