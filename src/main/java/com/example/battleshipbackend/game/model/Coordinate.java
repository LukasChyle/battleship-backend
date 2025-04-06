package com.example.battleshipbackend.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Coordinate {
    private int row;
    private int column;
}
