package com.example.battleshipbackend.game.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Log4j2
@Getter
@Setter
@AllArgsConstructor
public class GameSession {

  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService executorService;

  public GameSession(ScheduledExecutorService executorService, ObjectMapper objectMapper) {
    this.executorService = executorService;
    this.objectMapper = objectMapper;
  }

  private ScheduledFuture<?> scheduledFuture;
  private String id;
  private GameStateType gameState = GameStateType.WAITING_OPPONENT;
  private WebSocketSession sessionPlayer1;
  private WebSocketSession sessionPlayer2;
  private boolean isPlayer1Connected = false;
  private boolean isPlayer2Connected = false;
  private List<Strike> strikesPlayer1 = new ArrayList<>();
  private List<Strike> strikesPlayer2 = new ArrayList<>();
  private List<String> positionsPlayer1 = new ArrayList<>();
  private List<String> positionsPlayer2 = new ArrayList<>();
  private List<Ship> shipsPlayer1 = new ArrayList<>();
  private List<Ship> shipsPlayer2 = new ArrayList<>();

  public void startTimer() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    scheduledFuture = executorService.schedule(this::executeTimeout, 180, TimeUnit.SECONDS);
  }

  public Long getTimeLeft() {
    return scheduledFuture.getDelay(TimeUnit.SECONDS);
  }

  public void removeTimer() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
  }

  private void executeTimeout() {
    String message1 = "";
    String message2 = "";
    try {
      if (gameState == GameStateType.TURN_PLAYER1) {
        message1 = objectMapper.writeValueAsString(GameEvent.builder()
            .eventType(GameEventType.TIMEOUT_OWN)
            .build());
        message2 = objectMapper.writeValueAsString(GameEvent.builder()
            .eventType(GameEventType.TIMEOUT_OPPONENT)
            .build());
      } else {
        message1 = objectMapper.writeValueAsString(GameEvent.builder()
            .eventType(GameEventType.TIMEOUT_OPPONENT)
            .build());
        message2 = objectMapper.writeValueAsString(GameEvent.builder()
            .eventType(GameEventType.TIMEOUT_OWN)
            .build());
      }
    } catch (JsonProcessingException e) {
      log.error("GameEventToMessage: error processing JSON for WebSocket message: {}", e.getMessage());
    }
    if (isPlayer1Connected()) {
      sessionPlayer1.send(Mono.just(sessionPlayer1.textMessage(message1)))
          .then(sessionPlayer1.close()).subscribe();
    }
    if (sessionPlayer2 != null && isPlayer2Connected()) {
      sessionPlayer2.send(Mono.just(sessionPlayer2.textMessage(message2)))
          .then(sessionPlayer2.close()).subscribe();
    }
  }
}
