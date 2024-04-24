package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class GameCommand {
  @JsonProperty("type")
  private GameCommandType type;
  @JsonProperty("gameId")
  private String gameId;
  @JsonProperty("content")
  private String content;
  @JsonProperty("ships")
  private List<Ship> ships;
}
