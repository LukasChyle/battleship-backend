package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {
  @JsonProperty("gameId")
  private String gameId;
  @JsonProperty("ownStrikes")
  private List<Strike> ownStrikes;
  @JsonProperty("opponentStrikes")
  private List<Strike> opponentStrikes;
  @JsonProperty("strikeRow")
  private String strikeRow;
  @JsonProperty("strikeCol")
  private String strikeCol;
  @JsonProperty("isHit")
  private boolean isHit;
  @JsonProperty("type")
  private GameEventType type;
  @JsonProperty("ships")
  private List<Ship> ships;
}
