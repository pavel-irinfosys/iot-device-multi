package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceLatestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceLatestDataStore {

  private static final String LATEST_DATA_KEY = "devices:latest";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public void update(DeviceLatestData data) {
    try {
      redisTemplate.opsForHash().put(
          LATEST_DATA_KEY,
          data.imei(),
          objectMapper.writeValueAsString(data)
      );
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Failed to serialize device data", error);
    }
  }

  public Optional<DeviceLatestData> findByImei(String imei) {
    Object value = redisTemplate.opsForHash().get(LATEST_DATA_KEY, imei);

    if (value == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(
          objectMapper.readValue(value.toString(), DeviceLatestData.class)
      );
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Failed to deserialize device data", error);
    }
  }

  public List<String> findAll() {
    return redisTemplate.opsForHash()
        .keys(LATEST_DATA_KEY)
        .stream()
        .map(String::valueOf)
        .toList();
  }
}
