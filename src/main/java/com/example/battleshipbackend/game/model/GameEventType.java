package com.example.battleshipbackend.game.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GameEventType {
    WAITING_OPPONENT("WAITING_OPPONENT"),
    TURN_OWN("TURN_OWN"),
    TURN_OPPONENT("TURN_OPPONENT"),
    WON("WON"),
    LOST("LOST"),
    OPPONENT_LEFT("OPPONENT_LEFT");

    private final String value;

    GameEventType(String value) {
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
    public static GameEventType fromValue(String value) {
        for (GameEventType b : GameEventType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
