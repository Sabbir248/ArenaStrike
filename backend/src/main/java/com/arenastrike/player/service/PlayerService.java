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

    @Transactional(noRollbackFor = org.springframework.dao.DataIntegrityViolationException.class)
    public Player createGuest(String displayName) {
        String username = displayName.trim();
        try {
            return playerRepository.saveAndFlush(new Player(username));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String fallback = (username + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .substring(0, Math.min(32, username.length() + 9));
            return playerRepository.saveAndFlush(new Player(fallback));
        }
    }
}
