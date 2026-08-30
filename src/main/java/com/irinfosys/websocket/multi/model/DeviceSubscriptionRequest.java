package com.irinfosys.websocket.multi.model;

import java.util.List;

public record DeviceSubscriptionRequest(
    List<String> imeis) {

}
