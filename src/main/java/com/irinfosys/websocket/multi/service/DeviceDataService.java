package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceLatestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceDataService {

  private final DeviceLatestDataStore dataStore;
  private final StringRedisTemplate redisTemplate;
  private final ChannelTopic deviceDataTopic;
  private final ObjectMapper objectMapper;

  public void receive(String imei, String command, Object data) {
    DeviceLatestData latestData =
        new DeviceLatestData(imei, command, data, Instant.now());

    dataStore.update(latestData);
    publishDeviceEvent(latestData);
  }

  private void publishDeviceEvent(DeviceLatestData latestData) {
    try {
      redisTemplate.convertAndSend(
          deviceDataTopic.getTopic(),
          objectMapper.writeValueAsString(latestData)
      );
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Failed to publish device data", error);
    }
  }

}
