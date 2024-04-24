package com.example.battleshipbackend.webSocket.model;

import lombok.Getter;

@Getter
public enum GameStateType {
  WAITING_OPPONENT("WAITING_SECOND_PLAYER"),
  TURN_PLAYER1("TURN_PLAYER1"),
  TURN_PLAYER2("TURN_PLAYER2"),
  GAME_OVER("GAME_OVER");

  private final String value;

  GameStateType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
