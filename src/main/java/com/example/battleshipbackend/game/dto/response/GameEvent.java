package com.example.battleshipbackend.game.dto.response;

import com.example.battleshipbackend.game.dto.ShipDTO;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.model.Strike;
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
