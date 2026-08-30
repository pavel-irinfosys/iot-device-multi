package com.irinfosys.websocket.multi.model;

import java.time.Instant;

public record DeviceLatestData(
    String imei,
    String command,
    Object data,
    Instant receivedAt) {

}
