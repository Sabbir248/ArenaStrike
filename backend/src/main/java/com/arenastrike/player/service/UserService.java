package com.arenastrike.player.service;

import com.arenastrike.player.model.*;
import com.arenastrike.player.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User user = userRepository.save(new User(displayName.trim()));
        playerStatsRepository.save(new PlayerStats(user));
        return user;
    }
}
