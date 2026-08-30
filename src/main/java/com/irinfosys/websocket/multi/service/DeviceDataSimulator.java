package com.irinfosys.websocket.multi.service;

import com.irinfosys.websocket.multi.model.DeviceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceDataSimulator {

  private final Faker faker = new Faker();
  private final int DEVICE_COUNT = 10;
  private final List<DeviceInfo> DEVICE_IMEI_LIST = IntStream.range(0, DEVICE_COUNT)
      .mapToObj(index -> new DeviceInfo(
          String.format(
              "868484076%06d",
              index
          ),
          faker.number().numberBetween(400, 1900)
      ))
      .toList();

  private static final ThreadLocal<Faker> FAKER =
      ThreadLocal.withInitial(Faker::new);

  private final DeviceDataService deviceDataService;

  @Qualifier("deviceUploadExecutor")
  private final TaskExecutor deviceUploadExecutor;

  @Value("${app.instance-id:local}")
  private String instanceId;
//
//  @Scheduled(
//      fixedDelayString = "${device.simulator.interval-ms:1000}"
//  )
//  public void uploadDeviceData() {
//    final long currentTime =
//        System.currentTimeMillis();
//    DEVICE_IMEI_LIST.stream()
//        .filter(device -> device.isReadyForUpload(currentTime))
//        .forEach(device -> deviceUploadExecutor.execute(() -> updateDevice(device)
//            )
//        );
//  }

  private void updateDevice(DeviceInfo device) {
    final Faker faker = FAKER.get();

    final Map<String, Object> data = Map.of(
        "instanceId", instanceId,
        "status", faker.options()
            .option("ONLINE", "IDLE", "CHARGING"),
        "signal", faker.number()
            .numberBetween(2, 33),
        "power", faker.number()
            .randomDouble(2, 100, 5000),
        "temperature", faker.number()
            .randomDouble(2, 20, 70),
        "thread", Thread.currentThread().getName()
    );
    deviceDataService.receive(
        device.getImei(),
        "H0",
        data
    );
  }
}
