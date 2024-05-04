package com.example.battleshipbackend.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;

@Getter
@Setter
@AllArgsConstructor
public class GameSession {

  private final ScheduledExecutorService executorService;

  public GameSession(ScheduledExecutorService executorService) {
    this.executorService = executorService;
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
    scheduledFuture = executorService.schedule(this::executeTimedMethod, 10, TimeUnit.SECONDS);
  }

  public Long getTimeLeft() {
    return scheduledFuture.getDelay(TimeUnit.SECONDS);
  }

  public void removeTimer() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    executorService.shutdown();
  }

  private void executeTimedMethod() {
    System.out.println("executed timed method  id: " + id );


  }
}
