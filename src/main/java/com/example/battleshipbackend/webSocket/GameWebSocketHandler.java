package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.service.GameService;
import com.example.battleshipbackend.game.model.GameCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Component
public class GameWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper;
  private final GameService gameService;

  @Autowired
  public GameWebSocketHandler(ObjectMapper objectMapper, GameService gameService) {
    this.objectMapper = objectMapper;
    this.gameService = gameService;
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    log.info("Created WebSocketSession <{}>", session.getId());
    return session.receive()
        .onErrorResume(throwable -> {
          log.error("WebSocketSession <{}>: <{}>", session.getId(), throwable.toString());
          if (throwable instanceof IOException) { // Todo: might need more exception catching to hande all types of client disconnections, ReactorNettyException?
            log.error("Client disconnected from session <{}>", session.getId());
            return Flux.empty();
          }
          return Flux.error(throwable);
        })
        .flatMap(message -> {
          GameCommand command;
          try {
            command = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
          } catch (JsonProcessingException e) {
            log.error("Cast to GameCommand object error <{}>", e.getMessage());
            return session.send(Mono.just("Error: could not convert json to GameCommand object").map(session::textMessage));
          }
          if (command.getGameId() == null) {
            command.setGameId("");
          }
          return switch (command.getType()) {
            case STRIKE -> gameService.handleStrikeRequest(session, command);
            case JOIN -> gameService.handleJoinRequest(session, command);
            case LEAVE -> gameService.handleLeaveRequest(session, command);
            case RECONNECT -> gameService.handleReconnectRequest(session, command);
          };
        })
        .then(session.close())
        .doFinally(signal -> {
          log.info("Closed WebSocketSession <{}>", session.getId());
          gameService.handleClosedSession(session);
        });
  }
}

