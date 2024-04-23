package com.example.battleshipbackend.webSocket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GameStateType {
    WAITING_OPPONENT("WAITING_OPPONENT"),
    TURN_PLAYER1("OWN_TURN"),
    TURN_PLAYER2("OPPONENT_TURN"),
    WON_PLAYER1("WON"),
    WON_PLAYER2("LOST");

    private final String value;

    GameStateType(String value) {
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
    public static GameStateType fromValue(String value) {
        for (GameStateType b : GameStateType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
