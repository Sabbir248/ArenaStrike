package com.arenastrike.lobby.dto;

import com.arenastrike.lobby.model.GameMap;
import jakarta.validation.constraints.*;

public record CreateLobbyRequest(
        @NotBlank @Size(max = 32) String displayName,
        @NotNull GameMap map,
        @Min(2) @Max(10) int playerLimit
) {
}
