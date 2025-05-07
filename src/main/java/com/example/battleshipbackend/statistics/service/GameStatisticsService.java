package com.example.battleshipbackend.statistics.service;

import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.statistics.model.GameStatistics;
import reactor.core.publisher.Mono;

public interface GameStatisticsService {
  Mono<GameStatisticsDTO> getGameStatistics();

  Mono<Void> saveGameStatistics(GameStatistics statistics);
}
