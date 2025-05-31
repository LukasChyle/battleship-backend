package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.webSocket.WebSocketSessionRegistry;
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
  private final WebSocketSessionRegistry webSocketSessionRegistry;

  @Autowired
  public GameMessageServiceImpl(ObjectMapper objectMapper, WebSocketSessionRegistry webSocketSessionRegistry) {
    this.objectMapper = objectMapper;
    this.webSocketSessionRegistry = webSocketSessionRegistry;
  }

  @Override
  public Mono<Void> sendGameEventMessages(
    GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2, boolean lastMessage) {
    Mono<Void> messageFlow;
    try {
      webSocketSessionRegistry.sendToSession(session1.getId(), session1.textMessage(objectMapper.writeValueAsString(event1)));
      webSocketSessionRegistry.sendToSession(session2.getId(), session2.textMessage(objectMapper.writeValueAsString(event2)));
      messageFlow = Mono.empty();
    } catch (JsonProcessingException e) {
      log.error("GameEventsToMessages: error processing JSON for WebSocket message: {}", e.getMessage());
      messageFlow = Mono.when(
        sendStringMessage(session1, "Error: something went wrong server-side"),
        sendStringMessage(session2, "Error: something went wrong server-side")
      );
    }
    if (lastMessage) {
      messageFlow = messageFlow
        .then(session1.send(Mono.empty()).then(session1.close()))
        .then(session2.send(Mono.empty()).then(session2.close()));
    }
    return messageFlow;
  }

  @Override
  public Mono<Void> sendGameEventMessage(GameEvent event, WebSocketSession session, boolean lastMessage) {
    Mono<Void> messageFlow;
    try {
      webSocketSessionRegistry.sendToSession(session.getId(), session.textMessage(objectMapper.writeValueAsString(event)));
      messageFlow = Mono.empty();
    } catch (JsonProcessingException e) {
      log.error("GameEventToMessage: error processing JSON for WebSocket message: {}", e.getMessage());
      messageFlow = sendStringMessage(session, "Error message");
    }
    if (lastMessage) {
      messageFlow = messageFlow.then(session.send(Mono.empty()).then(session.close()));
    }
    return messageFlow;
  }

  @Override
  public Mono<Void> sendStringMessage(WebSocketSession session, String string) {
    if (webSocketSessionRegistry.hasSession(session.getId())) {
      webSocketSessionRegistry.sendToSession(session.getId(), session.textMessage(string));
    }
    return Mono.empty();
  }
}