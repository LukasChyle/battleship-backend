package com.example.battleshipbackend.game.model;

import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.enums.GameStateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Log4j2
@Getter
@Setter
public class GameSession {

  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService executorService;

  public GameSession(ScheduledExecutorService executorService, ObjectMapper objectMapper) {
    this.executorService = executorService;
    this.objectMapper = objectMapper;
  }

  private ScheduledFuture<?> scheduledFuture;
  private static final long TIMEOUT_DURATION = 120L;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

  private String id;
  private GameStateType gameState = GameStateType.WAITING_OPPONENT;
  private WebSocketSession sessionPlayer1;
  private WebSocketSession sessionPlayer2;
  private boolean isPlayer1Connected = false;
  private boolean isPlayer2Connected = false;
  private final List<Strike> strikesPlayer1 = new ArrayList<>();
  private final List<Strike> strikesPlayer2 = new ArrayList<>();
  private final List<Ship> activeShipsPlayer1 = new ArrayList<>();
  private final List<Ship> activeShipsPlayer2 = new ArrayList<>();
  private final List<Ship> sunkenShipsPlayer1 = new ArrayList<>();
  private final List<Ship> sunkenShipsPlayer2 = new ArrayList<>();



  public void startTimer() {
    removeTimer();
    scheduledFuture = executorService.schedule(
        this::executeTimeout,
        TIMEOUT_DURATION,
        TIMEOUT_UNIT
    );
  }

  public void removeTimer() {
    if (scheduledFuture != null && !scheduledFuture.isDone()) {
      scheduledFuture.cancel(false);
    }
  }

  public Long getTimeLeft() {
    if (scheduledFuture == null) {
      return 0L;
    }
    return scheduledFuture.getDelay(TimeUnit.SECONDS);
  }


  private void executeTimeout() {
    if (objectMapper == null) {
      log.error("ObjectMapper is null in executeTimeout");
      return;
    }
    try {
      GameEvent ownTimeoutEvent = GameEvent.builder()
          .eventType(GameEventType.TIMEOUT_OWN)
          .build();
      GameEvent opponentTimeoutEvent = GameEvent.builder()
          .eventType(GameEventType.TIMEOUT_OPPONENT)
          .build();
      String message1 = objectMapper.writeValueAsString(
          gameState == GameStateType.TURN_PLAYER1 ? ownTimeoutEvent : opponentTimeoutEvent
      );
      String message2 = objectMapper.writeValueAsString(
          gameState == GameStateType.TURN_PLAYER1 ? opponentTimeoutEvent : ownTimeoutEvent
      );
      if (isPlayer1Connected() && sessionPlayer1 != null) {
        sessionPlayer1.send(Mono.just(sessionPlayer1.textMessage(message1))).subscribe();
      }
      if (isPlayer2Connected() && sessionPlayer2 != null) {
        sessionPlayer2.send(Mono.just(sessionPlayer2.textMessage(message2))).subscribe();
      }
    } catch (JsonProcessingException e) {
      log.error("Failed to process timeout messages: {}", e.getMessage(), e);
    }
    closeAllSessions();
  }

  private void closeAllSessions() {
    if (isPlayer1Connected() && sessionPlayer1 != null && sessionPlayer1.isOpen()) {
      sessionPlayer1.close().subscribe();
    }
    if (isPlayer2Connected() && sessionPlayer2 != null && sessionPlayer2.isOpen()) {
      sessionPlayer2.close().subscribe();
    }
  }

  public void setActiveShipsPlayer1(List<Ship> ships) {
    this.activeShipsPlayer1.clear();
    this.activeShipsPlayer1.addAll(ships);
  }

  public void setActiveShipsPlayer2(List<Ship> ships) {
    this.activeShipsPlayer2.clear();
    this.activeShipsPlayer2.addAll(ships);
  }

  public void setSessionPlayer1(WebSocketSession session) {
    if (session == null) {
      throw new IllegalArgumentException("Session player 1 cannot be null");
    }
    this.sessionPlayer1 = session;
  }

  public void setSessionPlayer2(WebSocketSession session) {
    if (session == null) {
      throw new IllegalArgumentException("Session player 2 cannot be null");
    }
    this.sessionPlayer2 = session;
  }
}
