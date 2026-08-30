package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceLatestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceDataService {

  private final DeviceLatestDataStore dataStore;
  private final WebSocketPublisher webSocketPublisher;
  private final ClientDeviceSubscriptionStore subscriptionStore;

  public void receive(
      String imei,
      String command,
      Object data
  ) {
    final DeviceLatestData latestData = new DeviceLatestData(
        imei,
        command,
        data,
        Instant.now()
    );
    log.info(latestData.toString());
    dataStore.update(latestData);

    subscriptionStore.findSessionsSubscribedTo(imei)
        .forEach(sessionId -> webSocketPublisher.publishToSession(
            sessionId,
            "device-data",
            latestData
        ));
  }

}
