package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.entity.GameStatistics;
import com.example.battleshipbackend.game.repository.GameStatisticsRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GameStatisticsServiceImpl implements GameStatisticsService {

  private final GameStatisticsRepository repository;

  public GameStatisticsServiceImpl(GameStatisticsRepository repository) {
    this.repository = repository;
  }

  @Override
  public Mono<GameStatistics> getStatistics() {
    return repository.findById(1);
  }
}
