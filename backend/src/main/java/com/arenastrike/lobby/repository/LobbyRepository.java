package com.arenastrike.lobby.repository;

import com.arenastrike.lobby.model.Lobby;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    boolean existsByRoomCode(String roomCode);

    Optional<Lobby> findByRoomCode(String roomCode);

    @Query("select count(p) from LobbyParticipant p where p.lobby.roomCode = :roomCode")
    long countParticipantsByRoomCode(@Param("roomCode") String roomCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lobby l where l.roomCode = :roomCode")
    Optional<Lobby> findByRoomCodeForUpdate(@Param("roomCode") String roomCode);
}
