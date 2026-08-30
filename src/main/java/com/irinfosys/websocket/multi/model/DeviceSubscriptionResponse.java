package com.irinfosys.websocket.multi.model;

import java.util.List;

public record DeviceSubscriptionResponse(
    String sessionId,
    List<String> imeis) {

}
