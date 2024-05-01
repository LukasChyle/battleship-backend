package com.example.battleshipbackend.game.model;

import lombok.Data;

@Data
public class Ship {

  private String id;
  private Boolean isHorizontal;
  private int length;
  private int row;
  private int col;
}
