package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class GameMessageService {

  private final ObjectMapper objectMapper;

  @Autowired
  public GameMessageService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<Void> getGameEventsToMessages(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2,
      boolean lastMessage) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session1.send(Mono.just(objectMapper.writeValueAsString(event1)).map(session1::textMessage)));
      messages.add(session2.send(Mono.just(objectMapper.writeValueAsString(event2)).map(session2::textMessage)));

    } catch (JsonProcessingException e) {
      log.error("GameEventsToMessages: error processing JSON for WebSocket message: {}", e.getMessage());
      messages = new ArrayList<>();
      messages.add(getStringToMessage("Error: something went wrong server-side", session1));
      messages.add(getStringToMessage("Error: something went wrong server-side", session2));
    }
    if (lastMessage) {
      messages.add(session1.send(Mono.empty()).then(session1.close()));
      messages.add(session2.send(Mono.empty()).then(session2.close()));
    }
    return Flux.concat(messages).then();
  }

  public Mono<Void> getGameEventToMessage(GameEvent event, WebSocketSession session, boolean lastMessage) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session.send(Mono.just(objectMapper.writeValueAsString(event)).map(session::textMessage)));
    } catch (JsonProcessingException e) {
      log.error("GameEventToMessage: error processing JSON for WebSocket message: {}", e.getMessage());
      return getStringToMessage("Error: something went wrong server-side", session);
    }
    if (lastMessage) {
      messages.add(session.send(Mono.empty()).then(session.close()));
    }
    return Flux.concat(messages).then();
  }

  public Mono<Void> getStringToMessage(String string, WebSocketSession session) {
    return session.send(Mono.just(string).map(session::textMessage));
  }

}
