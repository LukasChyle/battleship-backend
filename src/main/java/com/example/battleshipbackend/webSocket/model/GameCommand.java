package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GameCommand {
  @JsonProperty("type")
  private RequestType type;
  @JsonProperty("gameId")
  private String gameId;
  @JsonProperty("content")
  private String content;
}
