package com.example.battleshipbackend.game.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipDTO {

  private String id; // needed or possible to remove?
  private Boolean isHorizontal;
  private int length;
  private int row;
  private int column;
}
