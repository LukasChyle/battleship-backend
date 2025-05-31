package com.example.battleshipbackend.webSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Component
public class WebSocketSessionRegistry {

  private final Map<String, Many<WebSocketMessage>> sessionSinks = new ConcurrentHashMap<>();

  public void register(String sessionId, Sinks.Many<WebSocketMessage> sink) {
    sessionSinks.put(sessionId, sink);
  }

  public void remove(String sessionId) {
    sessionSinks.remove(sessionId);
  }

  public void sendToSession(String sessionId, WebSocketMessage message) {
    var sink = sessionSinks.get(sessionId);
    if (sink != null) {
      sink.tryEmitNext(message);
    }
  }

  public boolean hasSession(String sessionId) {
    return sessionSinks.containsKey(sessionId);
  }
}
