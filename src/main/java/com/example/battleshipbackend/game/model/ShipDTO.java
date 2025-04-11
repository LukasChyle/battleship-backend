package com.example.battleshipbackend.game.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipDTO {

  private String id; // needed?
  private Boolean isHorizontal;
  private int length;
  private int row;
  private int column;
}
