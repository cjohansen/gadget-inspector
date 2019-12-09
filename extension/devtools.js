/*global browser, chrome*/

function getConsole() {
  if (isChrome) {
    try {
      return chrome.extension.getBackgroundPage().console;
    } catch (e) {
      // Wadda you gonna do?
    }
  }

  return console;
}

var panelWindow;
var queued;
const isChrome = typeof chrome !== 'undefined';
const ua = isChrome ? chrome : browser;
const konsole = getConsole();

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

const backgroundPageConnection = ua.runtime.connect({
  name: "devtools-page"
});

function setWindowSize(win) {
  backgroundPageConnection.postMessage({
    tabId: ua.devtools.inspectedWindow.tabId,
    name: "resize",
    height: win.innerHeight,
    width: win.innerWidth
  });
}

ua.devtools.panels.create(
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
  tabId: ua.devtools.inspectedWindow.tabId,
  name: "init"
});

ua.devtools.network.onNavigated.addListener(url => {
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
