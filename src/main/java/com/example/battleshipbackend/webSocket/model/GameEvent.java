package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GameEvent {
  @JsonProperty("gameId")
  private String gameId;
  @JsonProperty("ownStrikes")
  private List<String> ownStrikes;
  @JsonProperty("opponentStrikes")
  private List<String> opponentStrikes;
  @JsonProperty("type")
  private GameStateType type;
}
