package com.example.battleshipbackend.controller;

import com.example.battleshipbackend.game.dto.ActiveGamesDTO;
import com.example.battleshipbackend.game.service.GameService;
import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.statistics.service.GameStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GameController {

  private final GameStatisticsService gameStatisticsService;
  private final GameService gameService;

  @Autowired
  public GameController(GameStatisticsService gameStatisticsService, GameService gameService) {
    this.gameStatisticsService = gameStatisticsService;
    this.gameService = gameService;
  }

  @GetMapping("/game-statistics")
  public Mono<GameStatisticsDTO> getGameStatistics() {
    return gameStatisticsService.getGameStatistics();
  }

  @GetMapping("/current-games")
  public Mono<ActiveGamesDTO> getCurrentGames() {
    return gameService.getActiveGamesCount();
  }

  @GetMapping("/server-status")
  public Mono<ResponseEntity<String>> checkServerStatus() {
    return Mono.just(ResponseEntity.ok("Server is online"));
  }
}
