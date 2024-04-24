package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Ship {
  @JsonProperty("isHorizontal")
  private boolean isHorizontal;
  @JsonProperty("length")
  private int length;
  @JsonProperty("row")
  private String row;
  @JsonProperty("col")
  private String col;
}