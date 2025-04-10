package com.example.battleshipbackend.game.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameEvent {

  private String gameId;
  private GameEventType eventType;
  private List<Strike> ownStrikes;
  private List<Strike> opponentStrikes;
  private Long timeLeft;
  private List<ShipDTO> ownActiveShips;
  private List<ShipDTO> ownSunkenShips;
  private List<ShipDTO> opponentSunkenShips;
}
