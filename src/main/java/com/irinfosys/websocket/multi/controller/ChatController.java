package com.irinfosys.websocket.multi.controller;

import com.irinfosys.websocket.multi.model.ChatMessage;
import com.irinfosys.websocket.multi.model.DeviceDataUploadRequest;
import com.irinfosys.websocket.multi.model.DeviceSubscriptionRequest;
import com.irinfosys.websocket.multi.model.DeviceSubscriptionResponse;
import com.irinfosys.websocket.multi.service.ClientDeviceSubscriptionStore;
import com.irinfosys.websocket.multi.service.DeviceDataService;
import com.irinfosys.websocket.multi.service.DeviceLatestDataStore;
import com.irinfosys.websocket.multi.service.WebSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final DeviceLatestDataStore dataStore;
  private final ClientDeviceSubscriptionStore subscriptionStore;
  private final WebSocketPublisher webSocketPublisher;
  private final DeviceDataService deviceDataService;

  @MessageMapping("/devices.imeis")
  @SendToUser("/queue/device-imeis")
  public List<String> getAllImei() {
    List<String> allImei = dataStore.findAll();
    log.info(allImei.toString());
    return allImei;
  }

  @MessageMapping("/devices.subscribe")
  @SendToUser("/queue/device-subscriptions")
  public DeviceSubscriptionResponse subscribeDeviceImeis(
      @Valid DeviceSubscriptionRequest request,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    final String sessionId = headerAccessor.getSessionId();
    final List<String> subscribedImeis =
        subscriptionStore.subscribe(sessionId, request.imeis());

    log.info(
        "Session {} subscribed to device IMEIs {}",
        sessionId,
        subscribedImeis
    );

    subscribedImeis.forEach(imei -> dataStore.findByImei(imei)
        .ifPresent(latestData -> webSocketPublisher.publishToSession(
            sessionId,
            "device-data",
            latestData
        )));

    return new DeviceSubscriptionResponse(
        sessionId,
        subscribedImeis
    );
  }

  @MessageMapping("/chat.send")
  @SendTo("/topic/messages")
  public ChatMessage sendMessage(
      @Valid ChatMessage param
  ) {
    ChatMessage msg =  new ChatMessage(
        param.sender(),
        param.content(),
        Instant.now()
    );
    System.out.println(msg);
    return msg;
  }

  @MessageMapping("/devices.data")
  public void receiveDeviceData(
      @Valid DeviceDataUploadRequest request
  ) {
    deviceDataService.receive(
        request.imei(),
        request.command() == null || request.command().isBlank()
            ? "H0"
            : request.command(),
        request.data()
    );
  }


}
