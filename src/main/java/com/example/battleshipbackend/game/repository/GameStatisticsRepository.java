package com.example.battleshipbackend.game.repository;

import com.example.battleshipbackend.game.entity.GameStatistics;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStatisticsRepository extends ReactiveCrudRepository<GameStatistics, Integer> {
}
