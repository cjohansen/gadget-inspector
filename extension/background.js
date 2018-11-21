/*global chrome*/

const connections = {};

chrome.runtime.onConnect.addListener(port => {
  const extensionListener = (message, sender, sendResponse) => {
    if (message.name == "init") {
      connections[message.tabId] = port;
    }
  };

  port.onMessage.addListener(extensionListener);

  port.onDisconnect.addListener(port => {
    port.onMessage.removeListener(extensionListener);
    const tabs = Object.keys(connections);

    for (let i = 0, len = tabs.length; i < len; i++) {
      if (connections[tabs[i]] == port) {
        delete connections[tabs[i]]
        break;
      }
    }
  });
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (sender.tab) {
    const tabId = sender.tab.id;

    if (tabId in connections) {
      connections[tabId].postMessage(request);
    } else {
      console.log("Tab not found in connection list.");
    }
  } else {
    console.log("sender.tab not defined.");
  }

  return true;
});
