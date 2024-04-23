package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestType {
  JOIN("JOIN"),
  LEAVE("LEAVE"),
  STRIKE("STRIKE");

  private final String value;

  RequestType(String value) {
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
  public static RequestType fromValue(String value) {
    for (RequestType b : RequestType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
