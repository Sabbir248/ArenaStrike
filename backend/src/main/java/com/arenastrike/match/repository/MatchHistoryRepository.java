package com.arenastrike.match.repository;

import com.arenastrike.match.model.MatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchHistoryRepository extends JpaRepository<MatchHistory, Long> {
}
