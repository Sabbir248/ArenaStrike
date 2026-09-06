import { WEBSOCKET_URL } from "../config.js";

export class ArenaStrikeSocket {
    constructor(endpoint = WEBSOCKET_URL) {
        this.endpoint = endpoint;
        this.client = null;
    }

    connect(roomCode, player, onState, onStatus, onKill, onMatchOver, onExit) {
        if (!window.StompJs || !window.SockJS) {
            throw new Error("STOMP and SockJS scripts are not loaded");
        }
        this.client = new window.StompJs.Client({
            webSocketFactory: () => new window.SockJS(this.endpoint),
            reconnectDelay: 3000,
            onConnect: () => {
                onStatus("Connected");
                this.client.subscribe(`/topic/rooms/${roomCode}/state`, message => onState(JSON.parse(message.body)));
                this.client.subscribe(`/topic/rooms/${roomCode}/kills`, message => onKill(JSON.parse(message.body)));
                this.client.subscribe(`/topic/rooms/${roomCode}/match-over`, message => onMatchOver(JSON.parse(message.body)));
                this.client.subscribe(`/topic/rooms/${roomCode}/exits`, message => onExit(JSON.parse(message.body)));
                this.client.publish({
                    destination: `/app/rooms/${roomCode}/join`,
                    body: JSON.stringify({ player })
                });
            },
            onDisconnect: () => onStatus("Disconnected"),
            onStompError: frame => onStatus(`Broker error: ${frame.headers.message || "unknown"}`)
        });
        this.client.onWebSocketError = () => onStatus("WebSocket connection failed");
        onStatus("Connecting...");
        this.client.activate();
    }

    publishState(roomCode, state) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/state`,
                body: JSON.stringify(state)
            });
        }
    }

    shoot(roomCode, shooterId, targetId, hitLocation, weapon = "ASSAULT_RIFLE") {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/shoot`,
                body: JSON.stringify({ shooterId, targetId, hitLocation, weapon })
            });
        }
    }

    disconnect(roomCode, playerId) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/leave`,
                body: JSON.stringify(playerId)
            });
        }
        return this.client?.deactivate();
    }
}
