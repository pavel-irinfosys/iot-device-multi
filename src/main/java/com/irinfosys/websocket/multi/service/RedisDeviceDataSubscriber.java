package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceLatestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDeviceDataSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final ClientDeviceSubscriptionStore subscriptionStore;
  private final WebSocketPublisher webSocketPublisher;

  /**
   * Callback for processing received objects through Redis.
   *
   * @param message message must not be {@literal null}.
   * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
   */
  @Override
  public void onMessage(@NonNull final Message message, byte @Nullable [] pattern) {
    try {
      DeviceLatestData latestData =
          objectMapper.readValue(message.getBody(), DeviceLatestData.class);

      subscriptionStore.findSessionsSubscribedTo(latestData.imei())
          .forEach(sessionId -> webSocketPublisher.publishToSession(
              sessionId,
              "device-data",
              latestData
          ));
    } catch (Exception error) {
      log.error("Failed to handle Redis device data event", error);
    }
  }
}
