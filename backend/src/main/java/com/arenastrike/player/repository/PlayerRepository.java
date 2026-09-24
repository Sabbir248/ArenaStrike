package com.arenastrike.player.repository;

import com.arenastrike.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByUsername(String username);
    Optional<Player> findByUsername(String username);
    List<Player> findAllByOrderByWinsDescTotalKillsDesc();
}
