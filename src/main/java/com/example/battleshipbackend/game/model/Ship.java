package com.example.battleshipbackend.game.model;

import lombok.Data;

@Data
public class Ship {

  private String id;
  private boolean isHorizontal;
  private int length;
  private String row;
  private String col;
}