package com.irinfosys.websocket.multi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public void publish(
      String subscription,
      Object payload
  ) {
    messagingTemplate.convertAndSend(
        "/topic/" + subscription,
        payload
    );
  }

  public void publishToUser(
      String username,
      String subscription,
      Object payload
  ) {
    messagingTemplate.convertAndSendToUser(
        username,
        "/queue/" + subscription,
        payload
    );
  }

  public void publishToSession(
      String sessionId,
      String subscription,
      Object payload
  ) {
    final SimpMessageHeaderAccessor headerAccessor =
        SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);

    messagingTemplate.convertAndSendToUser(
        sessionId,
        "/queue/" + subscription,
        payload,
        headerAccessor.getMessageHeaders()
    );
  }
}
