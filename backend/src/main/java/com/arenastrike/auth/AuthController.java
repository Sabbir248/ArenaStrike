package com.arenastrike.auth;

import com.arenastrike.player.model.Player;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final com.arenastrike.player.service.PlayerService playerService;

    public AuthController(JwtService jwtService, com.arenastrike.player.service.PlayerService playerService) {
        this.jwtService = jwtService;
        this.playerService = playerService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = request.username() == null ? "Guest" : request.username().trim();
        Player player = playerService.createGuest(username);
        
        String token = jwtService.generateToken(player.getUsername(), player.getId());
        return ResponseEntity.ok(new AuthResponse(token, player.getId(), player.getUsername()));
    }

    public record AuthRequest(String username) {}
    public record AuthResponse(String token, Long playerId, String username) {}
}
