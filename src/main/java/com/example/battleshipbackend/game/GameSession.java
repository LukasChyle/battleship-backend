package com.example.battleshipbackend.game;

import com.example.battleshipbackend.webSocket.model.GameStateType;
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
    private WebSocketSession sessionPlayer1;
    private WebSocketSession sessionPlayer2;
    private GameStateType state;
}
