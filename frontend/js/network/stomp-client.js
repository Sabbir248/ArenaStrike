import { WEBSOCKET_URL } from "../config.js";

export class ArenaStrikeSocket {
    constructor(endpoint = WEBSOCKET_URL) {
        this.endpoint = endpoint;
        this.client = null;
    }

    connect(roomCode, player, onState, onStatus, onKill, onMatchOver, onExit, onHit, onTimer, onAmmo, onRespawn, onDisconnected, onLobbySync, onMatchStarted) {
        if (!window.StompJs || !window.SockJS) {
            throw new Error("STOMP and SockJS scripts are not loaded");
        }
        
        const token = sessionStorage.getItem("arena_jwt");
        const urlWithToken = token ? `${this.endpoint}?token=${token}` : this.endpoint;

        this.client = new window.StompJs.Client({
            webSocketFactory: () => new window.SockJS(urlWithToken),
            reconnectDelay: (count) => Math.min(3000 * Math.pow(2, count), 30000),
            heartbeatIncoming: 0, // Disabled to prevent premature client-side disconnection
            heartbeatOutgoing: 0,
            onConnect: () => {
                onStatus("Connected");
                
                const safeParse = (callback) => (message) => {
                    if (callback) {
                        try {
                            callback(JSON.parse(message.body));
                        } catch (e) {
                            console.error("Failed to parse STOMP message:", e, message.body);
                        }
                    }
                };

                this.client.subscribe(`/topic/room/${roomCode}`, safeParse(onState));
                this.client.subscribe(`/topic/rooms/${roomCode}/kills`, safeParse(onKill));
                this.client.subscribe(`/topic/room/${roomCode}/events`, safeParse((data) => {
                    if (data.event === "MATCH_STARTED" && onMatchStarted) {
                        onMatchStarted();
                    } else if (onHit) {
                        onHit(data);
                    }
                }));
                this.client.subscribe(`/topic/room/${roomCode}/start`, safeParse((data) => {
                    if (data.event === "MATCH_START" && onMatchStarted) {
                        onMatchStarted();
                    }
                }));
                this.client.subscribe(`/topic/room/${roomCode}/ammo`, safeParse(onAmmo));
                this.client.subscribe(`/topic/room/${roomCode}/game-over`, safeParse(onMatchOver));
                this.client.subscribe(`/topic/room/${roomCode}/timer`, safeParse(onTimer));
                this.client.subscribe(`/topic/rooms/${roomCode}/exits`, safeParse(onExit));
                this.client.subscribe(`/topic/room/${roomCode}/respawn`, safeParse(onRespawn));
                
                if (onLobbySync) {
                    this.client.subscribe(`/topic/lobby/${roomCode}`, (message) => {
                        console.log("LOBBY UPDATE RECEIVED:", message.body);
                        safeParse(onLobbySync)(message);
                    });
                }
                
                this.client.publish({
                    destination: `/app/rooms/${roomCode}/join`,
                    body: JSON.stringify({ player })
                });
            },
            onDisconnect: () => {
                console.log("STOMP client disconnected normally.");
                sessionStorage.removeItem('arena_room_id');
                localStorage.removeItem('arena_room_id');
                onStatus("Disconnected");
                onDisconnected?.();
            },
            onStompError: frame => {
                console.error("Broker reported error:", frame.headers.message);
                console.error("Additional details:", frame.body);
                onStatus(`Broker error: ${frame.headers.message || "unknown"}`);
            },
            onWebSocketClose: (evt) => {
                sessionStorage.removeItem('arena_room_id');
                localStorage.removeItem('arena_room_id');
                console.error(`WebSocket closed with code: ${evt.code}, reason: ${evt.reason}, clean: ${evt.wasClean}`);
            }
        });
        this.client.onWebSocketError = (evt) => {
            console.error("WebSocket error observed:", evt);
            onStatus("WebSocket connection failed");
        };
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
    
    startMatch(roomCode) {
        if (this.client?.connected) {
            this.client.publish({
                destination: `/app/lobby/${roomCode}/start`
            });
        }
    }
}
