package com.arenastrike.common;

import com.arenastrike.lobby.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(LobbyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(LobbyNotFoundException exception) {
        return new ErrorResponse("LOBBY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({LobbyConflictException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException exception) {
        return new ErrorResponse("LOBBY_CONFLICT", exception.getMessage());
    }

    public record ErrorResponse(String code, String message) {}
}
