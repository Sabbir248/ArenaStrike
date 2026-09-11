package com.arenastrike.player.service;

import com.arenastrike.player.model.*;
import com.arenastrike.player.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player createGuest(String displayName) {
        String username = displayName.trim();
        if (playerRepository.existsByUsername(username)) {
            username = (username + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .substring(0, Math.min(32, username.length() + 9));
        }
        return playerRepository.save(new Player(username));
    }
}
