/*global chrome*/

const connections = {};
const queue = {};

browser.runtime.onConnect.addListener(port => {
  const extensionListener = (message, sender, sendResponse) => {
    if (message.name == "init") {
      connections[message.tabId] = port;

      if (queue[message.tabId]) {
        console.log("Relaying %d queued messages", queue[message.tabId].length);
        while (queue[message.tabId].length > 0) {
          port.postMessage(queue[message.tabId].shift());
        }
      }
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

browser.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (sender.tab && request.id == "cljs-data-browser") {
    const tabId = sender.tab.id;

    if (tabId in connections) {
      console.log("Instantly relay message from", sender.tab.id);
      connections[tabId].postMessage(request);
    } else {
      console.log("Queue message from", sender.tab.id);
      queue[sender.tab.id] = [request];
    }
  }
  else if (request.id == "cljs-data-browser-2") {
    const {id, tabId, payload} = request;
    if (tabId && id === 'cljs-data-browser-2') {
      console.log('Instantly relay message from', tabId);
      browser.tabs.sendMessage(
        tabId,
        {message: payload}
      );
      
    }
  }

  return true;
});
