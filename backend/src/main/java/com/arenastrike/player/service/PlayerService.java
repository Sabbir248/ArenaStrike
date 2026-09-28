package com.arenastrike.player.service;

import com.arenastrike.player.model.*;
import com.arenastrike.player.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.text.Normalizer;
import java.util.Optional;

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

    @Transactional
    public Player findOrCreateOAuthPlayer(String provider, String subject) {
        return playerRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    String username = provider + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    return playerRepository.saveAndFlush(new Player(username, provider, subject));
                });
    }

    @Transactional(readOnly = true)
    public Optional<Player> findOAuthPlayer(String provider, String subject) {
        return playerRepository.findByOauthProviderAndOauthSubject(provider, subject);
    }

    @Transactional
    public Player saveNickname(Long playerId, String requestedNickname) {
        String nickname = Normalizer.normalize(requestedNickname, Normalizer.Form.NFC)
                .trim().replaceAll("\\s+", " ");
        if (nickname.length() < 3 || nickname.length() > 32) {
            throw new IllegalArgumentException("Nickname must be between 3 and 32 characters");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player account was not found"));
        if (playerRepository.existsByNicknameIgnoreCaseAndIdNot(nickname, playerId)) {
            throw new NicknameAlreadyTakenException();
        }

        player.setNickname(nickname);
        return playerRepository.saveAndFlush(player);
    }

    public static class NicknameAlreadyTakenException extends RuntimeException {
        public NicknameAlreadyTakenException() { super("Nickname already taken"); }
    }
}
