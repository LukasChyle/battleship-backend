package com.example.battleshipbackend.game.dto.request;

import com.example.battleshipbackend.game.dto.ShipDTO;
import com.example.battleshipbackend.game.enums.GameCommandType;
import java.util.List;
import lombok.Data;

@Data
public class GameCommand {

  private GameCommandType type;
  private String gameId;
  private Integer strikeRow;
  private Integer strikeColumn;
  private List<ShipDTO> ships;
}
