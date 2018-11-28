/*global chrome*/

const konsole = chrome.extension.getBackgroundPage().console;
var panelWindow;
var queued;


chrome.devtools.panels.create(
  "CLJS Data",
  "",
  "panel.html",
  panel => {
    panel.onShown.addListener(function onShown(panelWin) {
      panel.onShown.removeListener(onShown);
      panelWindow = panelWin;


      if (queued) {
        panelWindow.receiveMessage(queued);
        queued = null;
      }
    });
  }
);

const backgroundPageConnection = chrome.runtime.connect({
  name: "devtools-page"
});

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
