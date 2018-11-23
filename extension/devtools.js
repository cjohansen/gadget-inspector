/*global chrome*/

const konsole = chrome.extension.getBackgroundPage().console;
var panelWindow;
var queue = [];

chrome.devtools.panels.create(
  "Atoms",
  "MyPanelIcon.png",
  "panel.html",
  panel => {
    panel.onShown.addListener(function onShown(panelWin) {
      panel.onShown.removeListener(onShown);
      panelWindow = panelWin;

      while (queue.length > 0) {
        panelWindow.receiveMessage(queue.shift());
      }

      //panelWindow.sendMessage(msg => port.postMessage(msg));
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
        id: "cljs-atom-browser",
        type: "reset"
      }
    });
  } else {
    konsole.log("Empty queue", queue.length);
    queue = [];
  }
});

backgroundPageConnection.onMessage.addListener((request, sender, sendResponse) => {
  if (panelWindow) {
    panelWindow.receiveMessage({request, sender});
  } else {
    queue.push({request, sender});
  }
});
