import { WEBSOCKET_URL } from "../config.js";

export class ArenaStrikeSocket {
    constructor(endpoint = WEBSOCKET_URL) {
        this.endpoint = endpoint;
        this.client = null;
    }

    connect(roomCode, player, onState, onStatus, onKill, onMatchOver, onExit, onHit, onTimer, onAmmo, onDisconnected) {
        if (!window.StompJs || !window.SockJS) {
            throw new Error("STOMP and SockJS scripts are not loaded");
        }
        this.client = new window.StompJs.Client({
            webSocketFactory: () => new window.SockJS(this.endpoint),
            reconnectDelay: 3000,
            onConnect: () => {
                onStatus("Connected");
                this.client.subscribe(`/topic/room/${roomCode}`, message => onState(JSON.parse(message.body)));
                this.client.subscribe(`/topic/rooms/${roomCode}/kills`, message => onKill(JSON.parse(message.body)));
                this.client.subscribe(`/topic/room/${roomCode}/events`, message => onHit(JSON.parse(message.body)));
                this.client.subscribe(`/topic/room/${roomCode}/ammo`, message => onAmmo(JSON.parse(message.body)));
                this.client.subscribe(`/topic/room/${roomCode}/game-over`, message => onMatchOver(JSON.parse(message.body)));
                this.client.subscribe(`/topic/room/${roomCode}/timer`, message => onTimer(JSON.parse(message.body)));
                this.client.publish({
                    destination: `/app/rooms/${roomCode}/join`,
                    body: JSON.stringify({ player })
                });
            },
            onDisconnect: () => {
                onStatus("Disconnected");
                onDisconnected?.();
            },
            onStompError: frame => onStatus(`Broker error: ${frame.headers.message || "unknown"}`)
        });
        this.client.onWebSocketError = () => onStatus("WebSocket connection failed");
        onStatus("Connecting...");
        this.client.activate();
    }

    publishInput(roomCode, input) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/input`,
                body: JSON.stringify(input)
            });
        }
    }

    shoot(roomCode, shot) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/shoot`,
                body: JSON.stringify(shot)
            });
        }
    }

    reload(roomCode, command) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/rooms/${roomCode}/reload`,
                body: JSON.stringify(command)
            });
        }
    }

    disconnect(roomCode, playerId) {
        const disconnectingClient = this.client;
        if (disconnectingClient?.connected) {
            disconnectingClient.publish({
                destination: `/app/rooms/${roomCode}/leave`,
                body: JSON.stringify(playerId)
            });
        }
        this.client = null;
        return disconnectingClient?.deactivate();
    }
}
