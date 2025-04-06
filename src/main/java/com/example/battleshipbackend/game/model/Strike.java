package com.example.battleshipbackend.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Strike {
    private int row;
    private int column;
    private boolean isHit;
}
