package com.example.battleshipbackend.game.model;

import java.util.List;
import lombok.Data;

@Data
public class GameCommand {

  private GameCommandType type;
  private String gameId;
  private String row;
  private String column;
  private List<Ship> ships;
}
