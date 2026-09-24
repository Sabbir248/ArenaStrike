package com.arenastrike.auth;

import com.arenastrike.player.model.Player;
import com.arenastrike.player.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PlayerRepository playerRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.arenastrike.player.service.PlayerService playerService;

    public AuthController(PlayerRepository playerRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder, JwtService jwtService, com.arenastrike.player.service.PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.playerService = playerService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = request.username() == null ? "Guest" : request.username().trim();
        String password = request.password() == null ? "" : request.password();
        
        Player player;
        
        if (password.isEmpty()) {
            player = playerService.createGuest(username);
        } else {
            Optional<Player> playerOpt = playerRepository.findByUsername(username);
            if (playerOpt.isPresent()) {
                player = playerOpt.get();
                if (!passwordEncoder.matches(password, player.getPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } else {
                player = new Player(username, passwordEncoder.encode(password));
                player = playerRepository.save(player);
            }
        }
        
        String token = jwtService.generateToken(player.getUsername(), player.getId());
        return ResponseEntity.ok(new AuthResponse(token, player.getId(), player.getUsername()));
    }

    public record AuthRequest(String username, String password) {}
    public record AuthResponse(String token, Long playerId, String username) {}
}
