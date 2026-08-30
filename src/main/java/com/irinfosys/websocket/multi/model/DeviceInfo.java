package com.irinfosys.websocket.multi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@RequiredArgsConstructor
@ToString
public class DeviceInfo {

  private final String imei;
  private final long uploadIntervalMillis;

  private final AtomicLong lastUpdateTime =
      new AtomicLong(0);

  public boolean isReadyForUpload(long currentTime) {
    final long previousUpdateTime =
        lastUpdateTime.get();

    if (currentTime - previousUpdateTime
        < uploadIntervalMillis) {
      return false;
    }

    // Only one thread can mark the device for upload.
    return lastUpdateTime.compareAndSet(
        previousUpdateTime,
        currentTime
    );
  }

}
