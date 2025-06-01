package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.webSocket.WebSocketSinkRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class GameMessageServiceImpl implements GameMessageService {

  private final ObjectMapper objectMapper;
  private final WebSocketSinkRegistry webSocketSinkRegistry;

  @Autowired
  public GameMessageServiceImpl(ObjectMapper objectMapper, WebSocketSinkRegistry webSocketSinkRegistry) {
    this.objectMapper = objectMapper;
    this.webSocketSinkRegistry = webSocketSinkRegistry;
  }

  @Override
  public Mono<Void> sendGameEventMessages(
    GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2, boolean lastMessage) {
    Mono<Void> sendMono;
    try {
      webSocketSinkRegistry.send(session1.getId(), session1.textMessage(objectMapper.writeValueAsString(event1)));
      webSocketSinkRegistry.send(session2.getId(), session2.textMessage(objectMapper.writeValueAsString(event2)));
      sendMono = Mono.empty();
    } catch (JsonProcessingException e) {
      log.error("GameEventsToMessages: error processing JSON for WebSocket message: {}", e.getMessage());
      sendMono = Mono.when(
        sendStringMessage(session1, "Error: something went wrong server-side"),
        sendStringMessage(session2, "Error: something went wrong server-side")
      );
    }
    if (lastMessage) {
      return Mono.when(
        closeSessionAndSink(session1),
        closeSessionAndSink(session2)
      );
    }
    return sendMono;
  }

  @Override
  public Mono<Void> sendGameEventMessage(GameEvent event, WebSocketSession session, boolean lastMessage) {
    Mono<Void> sendMono;
    try {
      webSocketSinkRegistry.send(session.getId(), session.textMessage(objectMapper.writeValueAsString(event)));
      sendMono = Mono.empty();
    } catch (JsonProcessingException e) {
      log.error("GameEventToMessage: error processing JSON for WebSocket message: {}", e.getMessage());
      sendMono = sendStringMessage(session, "Error: something went wrong server-side");
    }
    if (lastMessage) {
      sendMono = sendMono.then(closeSessionAndSink(session));
    }
    return sendMono;
  }

  @Override
  public Mono<Void> sendStringMessage(WebSocketSession session, String string) {
    webSocketSinkRegistry.send(session.getId(), session.textMessage(string));
    return Mono.empty();
  }

  private Mono<Void> closeSessionAndSink(WebSocketSession session) {
    return session.close()
      .doOnSuccess(unused -> log.info("Closed session <{}>", session.getId()))
      .onErrorResume(error -> {
        log.warn("Error closing session <{}>: {}", session.getId(), error.getMessage());
        return Mono.empty();
      })
      .then(Mono.fromRunnable(() -> webSocketSinkRegistry.close(session.getId())));
  }
}