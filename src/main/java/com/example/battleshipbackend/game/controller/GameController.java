package com.example.battleshipbackend.game.controller;

import com.example.battleshipbackend.game.entity.GameStatistics;
import com.example.battleshipbackend.game.service.GameStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GameController {

  private final GameStatisticsService gameStatisticsService;

  @Autowired
  public GameController(GameStatisticsService gameStatisticsService) {
    this.gameStatisticsService = gameStatisticsService;
  }

  @GetMapping("/game-statistics")
  public Mono<GameStatistics> getGameStatistics() {
    return gameStatisticsService.getStatistics();
  }

  @GetMapping("/current-games")
  public Mono<ResponseEntity<String>> getCurrentGames() {
    // fetch data on how many current game sessions are active.
    return Mono.just(ResponseEntity.ok("placeholder"));
  }

  @GetMapping("/server-status")
  public Mono<ResponseEntity<String>> checkServerStatus() {
    return Mono.just(ResponseEntity.ok("Server is online"));
  }
}
