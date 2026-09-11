package com.arenastrike.realtime.dto;

public record RoomTimerEvent(String type, String roomId, long remainingSeconds) {
    public RoomTimerEvent(String roomId, long remainingSeconds) {
        this("TIMER_SYNC", roomId, remainingSeconds);
    }
}
