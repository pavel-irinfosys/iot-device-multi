package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceLatestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class DeviceLatestDataStore {

  private final ConcurrentMap<String, DeviceLatestData> latestDataByImei =
      new ConcurrentHashMap<>();

  public void update(DeviceLatestData data) {
    latestDataByImei.put(data.imei(), data);
  }

  public Optional<DeviceLatestData> findByImei(String imei) {
    return Optional.ofNullable(latestDataByImei.get(imei));
  }

  public List<String> findAll() {
    return List.copyOf(latestDataByImei.keySet());
  }

  public int size() {
    return latestDataByImei.size();
  }
}
