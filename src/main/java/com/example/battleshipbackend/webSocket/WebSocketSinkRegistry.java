package com.example.battleshipbackend.webSocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class WebSocketSinkRegistry {
  private final ConcurrentHashMap<String, FluxSink<WebSocketMessage>> sinkMap = new ConcurrentHashMap<>();

  public void register(String sessionId, FluxSink<WebSocketMessage> sink) {
    sinkMap.put(sessionId, sink);
  }

  public void close(String sessionId) {
    FluxSink<WebSocketMessage> sink = sinkMap.remove(sessionId);
    if (sink != null) {
      sink.complete();
    }
  }

  public void send(String sessionId, WebSocketMessage message) {
    FluxSink<WebSocketMessage> sink = sinkMap.get(sessionId);
    if (sink != null) {
      sink.next(message);
    }
  }
}
