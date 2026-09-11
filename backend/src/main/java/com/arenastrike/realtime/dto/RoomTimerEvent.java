package com.arenastrike.realtime.dto;

public record RoomTimerEvent(String roomId, long remainingSeconds) {
}
