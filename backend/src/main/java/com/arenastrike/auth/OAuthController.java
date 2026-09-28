package com.arenastrike.auth;

import com.arenastrike.player.model.Player;
import com.arenastrike.player.service.PlayerService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {
    private final OAuthIdentityVerifier identityVerifier;
    private final PlayerService playerService;
    private final JwtService jwtService;

    public OAuthController(OAuthIdentityVerifier identityVerifier, PlayerService playerService, JwtService jwtService) {
        this.identityVerifier = identityVerifier;
        this.playerService = playerService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public OAuthResponse login(@RequestBody OAuthRequest request) {
        System.out.println("👉 OAUTH LOGIN REQUEST REACHED CONTROLLER");
        OAuthIdentityVerifier.VerifiedIdentity identity = identityVerifier.verify(request.provider(), request.credential());
        Player player;
        try {
            player = playerService.findOrCreateOAuthPlayer(identity.provider(), identity.subject());
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            // A parallel login may have inserted this provider subject first.
            player = playerService.findOAuthPlayer(identity.provider(), identity.subject())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            HttpStatus.CONFLICT, "Unable to create the account; please retry"));
        }
        return new OAuthResponse(
                jwtService.generateToken(player.getUsername(), player.getId()),
                player.getId(), player.getUsername(), player.getNickname(), identity.name());
    }

    public record OAuthRequest(String provider, String credential) {}
    public record OAuthResponse(String token, Long playerId, String username, String nickname, String profileName) {}
}
