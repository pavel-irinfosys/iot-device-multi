package com.irinfosys.websocket.multi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class ClientDeviceSubscriptionStore {

  private final ConcurrentMap<String, Set<String>> imeisBySession =
      new ConcurrentHashMap<>();

  public List<String> subscribe(
      String sessionId,
      Collection<String> imeis
  ) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("Session id is required");
    }

    final LinkedHashSet<String> selectedImeis =
        normalizeImeis(imeis);

    if (selectedImeis.isEmpty()) {
      imeisBySession.remove(sessionId);
      return List.of();
    }

    imeisBySession.put(
        sessionId,
        Collections.unmodifiableSet(selectedImeis)
    );

    return List.copyOf(selectedImeis);
  }

  public List<String> findSessionsSubscribedTo(String imei) {
    return imeisBySession.entrySet()
        .stream()
        .filter(entry -> entry.getValue().contains(imei))
        .map(Map.Entry::getKey)
        .toList();
  }

  public void removeSession(String sessionId) {
    imeisBySession.remove(sessionId);
  }

  @EventListener
  public void removeDisconnectedSession(
      SessionDisconnectEvent event
  ) {
    removeSession(event.getSessionId());
    log.info("Removed device subscriptions for session {}", event.getSessionId());
  }

  private LinkedHashSet<String> normalizeImeis(
      Collection<String> imeis
  ) {
    final LinkedHashSet<String> selectedImeis =
        new LinkedHashSet<>();

    if (imeis == null) {
      return selectedImeis;
    }

    imeis.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(imei -> !imei.isBlank())
        .forEach(selectedImeis::add);

    return selectedImeis;
  }
}
