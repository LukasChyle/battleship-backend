package com.example.battleshipbackend.game;

import com.example.battleshipbackend.webSocket.model.GameStateType;
import com.example.battleshipbackend.webSocket.model.Ship;
import com.example.battleshipbackend.webSocket.model.Strike;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameSession {
    private String id;
    private GameStateType gameState = GameStateType.WAITING_OPPONENT;
    private WebSocketSession sessionPlayer1;
    private WebSocketSession sessionPlayer2;
    private List<Strike> strikesPlayer1 = new ArrayList<>();
    private List<Strike> strikesPlayer2 = new ArrayList<>();
    private List<String> positionsPlayer1 = new ArrayList<>();
    private List<String> positionsPlayer2 = new ArrayList<>();
    private List<Ship> shipsPlayer1 = new ArrayList<>();
    private List<Ship> shipsPlayer2 = new ArrayList<>();
}
