package com.example.battleshipbackend.controller;

import com.example.battleshipbackend.game.dto.ActiveGamesDTO;
import com.example.battleshipbackend.game.service.GameSessionService;
import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.statistics.service.GameStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class GameController {

  private final GameStatisticsService gameStatisticsService;
  private final GameSessionService gameSessionService;

  @Autowired
  public GameController(GameStatisticsService gameStatisticsService, GameSessionService gameSessionService) {
    this.gameStatisticsService = gameStatisticsService;
    this.gameSessionService = gameSessionService;
  }

  @GetMapping("/game-statistics")
  public Mono<GameStatisticsDTO> getGameStatistics() {
    return gameStatisticsService.getGameStatistics();
  }

  @GetMapping("/current-games")
  public Mono<ActiveGamesDTO> getCurrentGames() {
    return gameSessionService.getActiveGamesCount();
  }

  @GetMapping("/server-status")
  public Mono<ResponseEntity<String>> checkServerStatus() {
    return Mono.just(ResponseEntity.ok("UP"));
  }
}
