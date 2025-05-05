package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.entity.GameStatistics;
import reactor.core.publisher.Mono;

public interface GameStatisticsService {
  Mono<GameStatistics> getStatistics();

  // add methods to store statistic data.
}
