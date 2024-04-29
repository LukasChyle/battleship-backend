package com.example.battleshipbackend.game.model;

import lombok.Getter;

@Getter
public enum GameStateType {
  WAITING_OPPONENT("WAITING_SECOND_PLAYER"),
  TURN_PLAYER1("TURN_PLAYER1"),
  TURN_PLAYER2("TURN_PLAYER2");

  private final String value;

  GameStateType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
