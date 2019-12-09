/*global browser, chrome*/

const connections = {};
const ua = typeof chrome !== 'undefined' ? chrome : browser;

ua.runtime.onConnect.addListener(port => {
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

ua.runtime.onMessage.addListener((request, sender, sendResponse) => {
  const tabId = sender.tab.id;

  if (tabId in connections) {
    console.log("Instantly relay message from", tabId);
    connections[tabId].postMessage(request);
  }

  return true;
});
