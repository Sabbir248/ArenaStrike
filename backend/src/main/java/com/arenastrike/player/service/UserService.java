package com.arenastrike.player.service;

import com.arenastrike.player.model.*;
import com.arenastrike.player.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PlayerStatsRepository playerStatsRepository;

    public UserService(UserRepository userRepository, PlayerStatsRepository playerStatsRepository) {
        this.userRepository = userRepository;
        this.playerStatsRepository = playerStatsRepository;
    }

    @Transactional
    public User createGuest(String displayName) {
        String username = displayName.trim();
        if (userRepository.existsByUsername(username)) {
            username = (username + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .substring(0, Math.min(32, username.length() + 9));
        }
        User user = userRepository.save(new User(username));
        playerStatsRepository.save(new PlayerStats(user));
        return user;
    }
}
