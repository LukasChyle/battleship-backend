package com.example.battleshipbackend.game.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameEvent {

  private String gameId;
  private List<Strike> ownStrikes;
  private List<Strike> opponentStrikes;
  private String strikeRow;
  private String strikeCol;
  private boolean isHit;
  private GameEventType eventType;
  private List<Ship> ships;
}
