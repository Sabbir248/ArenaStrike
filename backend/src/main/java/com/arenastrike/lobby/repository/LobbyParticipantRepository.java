package com.arenastrike.lobby.repository;

import com.arenastrike.lobby.model.LobbyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LobbyParticipantRepository extends JpaRepository<LobbyParticipant, Long> {
}
