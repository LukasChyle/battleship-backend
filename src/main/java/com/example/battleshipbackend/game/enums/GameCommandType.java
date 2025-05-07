package com.example.battleshipbackend.game.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GameCommandType {
  JOIN("JOIN"),
  RECONNECT("RECONNECT"),
  LEAVE("LEAVE"),
  STRIKE("STRIKE");

  private final String value;

  GameCommandType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static GameCommandType fromValue(String value) {
    for (GameCommandType b : GameCommandType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
