package com.arenastrike.player.controller;

import com.arenastrike.auth.JwtService;
import com.arenastrike.player.model.Player;
import com.arenastrike.player.repository.PlayerRepository;
import com.arenastrike.player.service.PlayerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings/nickname")
public class SettingsController {
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final JwtService jwtService;

    public SettingsController(PlayerRepository playerRepository, PlayerService playerService, JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.playerService = playerService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public NicknameResponse getNickname(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Player player = getAuthenticatedPlayer(authorization);
        return new NicknameResponse(player.getNickname(), player.getDisplayName());
    }

    @PostMapping
    public NicknameResponse saveNickname(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody NicknameRequest request) {
        Player player = getAuthenticatedPlayer(authorization);
        try {
            Player updated = playerService.saveNickname(player.getId(), request.nickname());
            return new NicknameResponse(updated.getNickname(), updated.getDisplayName());
        } catch (PlayerService.NicknameAlreadyTakenException | DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nickname already taken");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private Player getAuthenticatedPlayer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        String token = authorization.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
        Long playerId = jwtService.extractPlayerId(token);
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player account was not found"));
    }

    public record NicknameRequest(@NotBlank @Size(min = 3, max = 32) String nickname) {}
    public record NicknameResponse(String nickname, String displayName) {}
}
