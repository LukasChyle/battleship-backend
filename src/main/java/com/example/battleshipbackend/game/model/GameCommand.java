package com.example.battleshipbackend.game.model;

import java.util.List;
import lombok.Data;

@Data
public class GameCommand {

  private GameCommandType type;
  private String gameId;
  private Integer row;
  private Integer column;
  private List<ShipDTO> ships;
}
