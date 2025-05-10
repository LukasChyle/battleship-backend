package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.converter.GameDtoConverter;
import com.example.battleshipbackend.game.service.GameService;
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
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Component
public class GameWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper;
  private final GameService gameService;
  private final GameDtoConverter gameDtoConverter;
  private static final int MAX_MESSAGES = 5;
  private static final Duration BUCKET_INTERVAL = Duration.ofSeconds(5);

  private final Map<String, AtomicInteger> sessionMessageCounters = new ConcurrentHashMap<>();

  @Autowired
  public GameWebSocketHandler(ObjectMapper objectMapper, GameService gameService, GameDtoConverter gameDtoConverter) {
    this.objectMapper = objectMapper;
    this.gameService = gameService;
    this.gameDtoConverter = gameDtoConverter;
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
    return session.receive()
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
          if (command.getType() == null) {
            return Flux.empty();
          }
          if (command.getGameId() == null) {
            command.setGameId("");
          }
          return switch (command.getType()) {
            case STRIKE -> gameService.handleStrikeRequest(session, command);
            case JOIN -> gameService.handleJoinRequest(session, command, gameDtoConverter.toListOfShip(command.getShips()));
            case JOIN_FRIEND -> gameService.handleJoinFriendRequest(session, command, gameDtoConverter.toListOfShip(command.getShips()));
            case LEAVE -> gameService.handleLeaveRequest(session, command);
            case RECONNECT -> gameService.handleReconnectRequest(session, command);
          };
        })
        .doFinally(signal -> {
          log.info("Closed WebSocketSession <{}>", session.getId());
          sessionMessageCounters.remove(session.getId());
          gameService.handleClosedSession(session)
              .doOnError(error -> log.error("Error in handleClosedSession: ", error))
              .onErrorComplete()
              .subscribe();
        })
        .then(session.close());
  }
}

