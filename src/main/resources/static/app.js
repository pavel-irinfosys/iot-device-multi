let client;
let liveDeviceDataByImei = {};

const connectButton = document.getElementById("connectButton");
const getDevicesButton = document.getElementById("getDevicesButton");
const instanceInfo = document.getElementById("instanceInfo");
const deviceSelection = document.getElementById("deviceSelection");
const deviceImeiSelect = document.getElementById("deviceImeiSelect");
const subscribeDevicesButton = document.getElementById("subscribeDevicesButton");
const output = document.getElementById("output");
const selectedOutput = document.getElementById("selectedOutput");
const subscriptionOutput = document.getElementById("subscriptionOutput");
const liveDataOutput = document.getElementById("liveDataOutput");

connectButton.addEventListener("click", connect);
getDevicesButton.addEventListener("click", getAllDevices);
deviceImeiSelect.addEventListener("change", showSelectedDeviceImeis);
subscribeDevicesButton.addEventListener("click", subscribeSelectedDevices);

function showGetDevicesButton() {
  getDevicesButton.hidden = false;
  getDevicesButton.disabled = false;
}

function hideGetDevicesButton() {
  getDevicesButton.hidden = true;
  getDevicesButton.disabled = true;
}

function hideDeviceSelection() {
  deviceSelection.hidden = true;
  deviceImeiSelect.disabled = true;
  deviceImeiSelect.innerHTML = "";
  subscribeDevicesButton.disabled = true;
  selectedOutput.hidden = true;
  selectedOutput.textContent = "";
  subscriptionOutput.hidden = true;
  subscriptionOutput.textContent = "";
  liveDataOutput.hidden = true;
  liveDataOutput.textContent = "";
  liveDeviceDataByImei = {};
}

function setDisconnectedState(message) {
  connectButton.disabled = false;
  connectButton.textContent = "Connect to Server";
  instanceInfo.textContent = "Java instance: Not connected";
  hideGetDevicesButton();
  hideDeviceSelection();
  output.textContent = message;
}

function populateDeviceSelection(devices) {
  deviceImeiSelect.innerHTML = "";

  devices.forEach(function (deviceImei) {
    const option = document.createElement("option");
    option.value = deviceImei;
    option.textContent = deviceImei;
    deviceImeiSelect.appendChild(option);
  });

  deviceSelection.hidden = false;
  deviceImeiSelect.disabled = false;
  subscribeDevicesButton.disabled = true;
  selectedOutput.hidden = false;
  selectedOutput.textContent = "Selected device IMEI: []";
}

function getSelectedDeviceImeis() {
  return Array.from(deviceImeiSelect.selectedOptions)
      .map(function (option) {
        return option.value;
      });
}

function showSelectedDeviceImeis() {
  const selectedImeis = getSelectedDeviceImeis();

  subscribeDevicesButton.disabled = selectedImeis.length === 0;
  selectedOutput.textContent =
      "Selected device IMEI: " + JSON.stringify(selectedImeis, null, 2);
}

function showSubscription(subscription) {
  subscriptionOutput.hidden = false;
  subscriptionOutput.textContent =
      "Backend subscription: " + JSON.stringify(subscription, null, 2);
}

function showLiveDeviceData(latestData) {
  liveDeviceDataByImei[latestData.imei] = latestData;
  liveDataOutput.hidden = false;
  liveDataOutput.textContent =
      "Subscribed device data: " + JSON.stringify(liveDeviceDataByImei, null, 2);
}

function getWebSocketUrl() {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return protocol + "//" + window.location.host + "/ws";
}

function loadConnectedInstanceInfo() {
  fetch("/api/instance", {
    cache: "no-store"
  })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("Instance lookup failed with status " + response.status);
        }
        return response.json();
      })
      .then(function (info) {
        instanceInfo.textContent =
            "Java instance: " + info.instanceId + " | Port: " + info.port;
      })
      .catch(function (error) {
        console.error("Instance lookup failed:", error);
        instanceInfo.textContent = "Java instance: Connected, info unavailable";
      });
}

function connect() {
  connectButton.disabled = true;
  output.textContent = "Connecting...";

  client = new StompJs.Client({
    brokerURL: getWebSocketUrl(),
    debug: function (message) {
      console.log(message);
    }
  });

  client.onConnect = function () {
    console.log("Connected");

    connectButton.textContent = "Connected";
    output.textContent = "Connected to server";
    showGetDevicesButton();
    loadConnectedInstanceInfo();

    client.subscribe(
        "/user/queue/device-imeis",
        function (message) {
          console.log("Message received:", message.body);

          const devices = JSON.parse(message.body);

          populateDeviceSelection(devices);
          output.textContent = JSON.stringify(devices, null, 2);
        }
    );

    client.subscribe(
        "/user/queue/device-subscriptions",
        function (message) {
          console.log("Subscription saved:", message.body);

          const subscription = JSON.parse(message.body);
          showSubscription(subscription);
        }
    );

    client.subscribe(
        "/user/queue/device-data",
        function (message) {
          console.log("Device data received:", message.body);

          const latestData = JSON.parse(message.body);
          showLiveDeviceData(latestData);
        }
    );
  };

  client.onStompError = function (frame) {
    console.error("STOMP error:", frame);

    setDisconnectedState("STOMP error: " + frame.headers.message);
  };

  client.onWebSocketError = function (error) {
    console.error("WebSocket error:", error);

    setDisconnectedState("WebSocket connection failed");
  };

  client.onWebSocketClose = function () {
    console.log("WebSocket disconnected");
    setDisconnectedState("Disconnected");
  };

  client.activate();
}

function getAllDevices() {
  if (!client || !client.connected) {
    output.textContent = "Connect to server first";
    return;
  }

  output.textContent = "Loading devices...";
  hideDeviceSelection();

  // Calls @MessageMapping("/devices.imeis")
  client.publish({
    destination: "/app/devices.imeis",
    body: "{}"
  });
}

function subscribeSelectedDevices() {
  if (!client || !client.connected) {
    output.textContent = "Connect to server first";
    return;
  }

  const selectedImeis = getSelectedDeviceImeis();
  if (selectedImeis.length === 0) {
    selectedOutput.textContent = "Select at least one device IMEI";
    return;
  }

  liveDeviceDataByImei = {};
  liveDataOutput.hidden = false;
  liveDataOutput.textContent = "Waiting for subscribed device data...";

  client.publish({
    destination: "/app/devices.subscribe",
    body: JSON.stringify({
      imeis: selectedImeis
    })
  });
}
