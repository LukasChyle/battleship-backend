package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.converter.GameDtoConverter;
import com.example.battleshipbackend.game.service.GameMessageService;
import com.example.battleshipbackend.game.service.GameSessionService;
import com.example.battleshipbackend.game.dto.request.GameCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Component
public class GameWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper;
  private final GameSessionService gameSessionService;
  private final GameDtoConverter gameDtoConverter;
  private final GameMessageService gameMessageService;
  private final WebSocketSinkRegistry webSocketSinkRegistry;
  private static final int MAX_MESSAGES = 20;
  private static final Duration BUCKET_INTERVAL = Duration.ofSeconds(5);

  private final Map<String, AtomicInteger> sessionMessageCounters = new ConcurrentHashMap<>();

  @Autowired
  public GameWebSocketHandler(
    ObjectMapper objectMapper,
    GameSessionService gameSessionService,
    GameDtoConverter gameDtoConverter,
    GameMessageService gameMessageService,
    WebSocketSinkRegistry webSocketSinkRegistry) {
    this.objectMapper = objectMapper;
    this.gameSessionService = gameSessionService;
    this.gameDtoConverter = gameDtoConverter;
    this.gameMessageService = gameMessageService;
    this.webSocketSinkRegistry = webSocketSinkRegistry;
    Schedulers.single()
      .schedulePeriodically(this::resetAllCounters, BUCKET_INTERVAL.toMillis(), BUCKET_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
  }

  private void resetAllCounters() {
    sessionMessageCounters.forEach((sessionId, counter) -> counter.set(0));
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    log.info("Created WebSocketSession <{}>", session.getId());
    sessionMessageCounters.putIfAbsent(session.getId(), new AtomicInteger(0));

    Flux<WebSocketMessage> outputFlux = Flux.<WebSocketMessage>create(sink -> {
      webSocketSinkRegistry.register(session.getId(), sink);
    }).doFinally(signal -> webSocketSinkRegistry.close(session.getId()));

    Mono<Void> input = session.receive()
      .filter(message -> message.getType() == WebSocketMessage.Type.TEXT)
      .doOnNext(message -> {
        AtomicInteger counter = sessionMessageCounters.get(session.getId());
        if (counter.incrementAndGet() > MAX_MESSAGES) {
          log.warn("Rate limit exceeded <{}> for session <{}>", counter, session.getId());
        }
      })
      .filter(message -> sessionMessageCounters.get(session.getId()).get() <= MAX_MESSAGES)
      .onErrorResume(throwable -> {
        log.error("WebSocketSession <{}>: <{}>", session.getId(), throwable.toString());
        if (throwable instanceof IOException) {
          log.error("Client disconnected from session <{}>", session.getId());
          return Mono.empty();
        }
        return Mono.error(throwable);
      })
      .flatMapSequential(message -> {
        String payload = message.getPayloadAsText().trim();
        if ("ping".equalsIgnoreCase(payload)) {
          return gameMessageService.sendStringMessage(session, "pong");
        }
        GameCommand command;
        try {
          command = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
        } catch (JsonProcessingException e) {
          log.error("Cast to GameCommand object error <{}>", e.getMessage());
          return gameMessageService.sendStringMessage(session, "Error: could not convert json to GameCommand object");
        }
        if (command.getType() == null) {
          return Mono.empty();
        }
        if (command.getGameId() == null) {
          command.setGameId("");
        }
        return switch (command.getType()) {
          case STRIKE -> gameSessionService.handleStrikeRequest(session, command);
          case JOIN -> gameSessionService.handleJoinRequest(session, command, gameDtoConverter.toListOfShip(command.getShips()));
          case JOIN_FRIEND ->
            gameSessionService.handleJoinFriendRequest(session, command, gameDtoConverter.toListOfShip(command.getShips()));
          case JOIN_AI -> gameSessionService.handleJoinAiRequest(session, command, gameDtoConverter.toListOfShip(command.getShips()));
          case LEAVE -> gameSessionService.handleLeaveRequest(session, command);
          case RECONNECT -> gameSessionService.handleReconnectRequest(session, command);
        };
      }).then();
    Mono<Void> output = session.send(outputFlux);
    return Mono.when(input, output).doFinally(signal -> {
      sessionMessageCounters.remove(session.getId());
      gameSessionService.handleClosedSession(session)
        .doOnError(error -> log.error("Error in handleClosedSession: ", error))
        .onErrorComplete()
        .subscribe();
    });
  }
}

