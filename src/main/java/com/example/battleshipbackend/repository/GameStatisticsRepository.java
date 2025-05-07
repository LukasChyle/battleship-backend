package com.example.battleshipbackend.repository;

import com.example.battleshipbackend.statistics.entity.GameStatisticsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStatisticsRepository extends ReactiveCrudRepository<GameStatisticsEntity, Integer> {
}
