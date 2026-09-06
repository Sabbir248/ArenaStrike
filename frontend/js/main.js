import { ArenaStrikeRenderer } from "./game/renderer.js";
import { ArenaStrikeSocket } from "./network/stomp-client.js";
import { API_BASE_URL } from "./config.js";
import { ArenaStrikeSoundManager } from "./audio/sound-manager.js";

const canvas = document.querySelector("#game-canvas");
const statusElement = document.querySelector("#connection-status");
const roomStatusElement = document.querySelector("#room-status");
const matchTimerElement = document.querySelector("#match-timer");
const killFeedElement = document.querySelector("#kill-feed");
const matchOverElement = document.querySelector("#match-over");
const renderer = new ArenaStrikeRenderer(canvas);
const socket = new ArenaStrikeSocket();
const sounds = new ArenaStrikeSoundManager();
const keys = new Set();
const query = new URLSearchParams(window.location.search);
let roomCode = query.get("room")?.toUpperCase() || "";
let playerId = Number(query.get("playerId")) || Math.floor(Math.random() * 900000) + 100000;
let displayName = query.get("name") || "";
let weapon = query.get("weapon") || "ASSAULT_RIFLE";
let selectedMap = query.get("map") || "MAP_1";
const movement = { forward: 0, strafe: 0 };
let yaw = 0;
let pitch = 0;
let lastTime = performance.now();
let lastNetworkUpdate = 0;
let verticalVelocity = 0;
let onGround = true;
let stance = "STANDING";

document.addEventListener("keydown", (event) => {
    if (["KeyW", "KeyA", "KeyS", "KeyD", "Space"].includes(event.code)) {
        event.preventDefault();
    }
    keys.add(event.code);
    if (event.code === "KeyZ") {
        stance = stance === "PRONE" ? "STANDING" : "PRONE";
    }
});
document.addEventListener("keyup", (event) => {
    if (["KeyW", "KeyA", "KeyS", "KeyD", "Space"].includes(event.code)) {
        event.preventDefault();
    }
    keys.delete(event.code);
});
window.addEventListener("blur", () => keys.clear());
canvas.addEventListener("click", () => {
    sounds.unlock();
    if (document.pointerLockElement !== canvas) {
        canvas.requestPointerLock();
    }
});
canvas.addEventListener("mousedown", (event) => {
    if (event.button === 2) {
        event.preventDefault();
        sounds.unlock();
        renderer.setAiming(true);
        return;
    }
    if (event.button !== 0 || document.pointerLockElement !== canvas) {
        return;
    }
    renderer.fireWeapon();
    sounds.fire();
    const hit = renderer.raycastOpponent();
    if (hit && socket.client?.connected) {
        socket.shoot(roomCode, playerId, hit.targetId, hit.hitLocation, weapon);
    }
});
canvas.addEventListener("mouseup", (event) => {
    if (event.button === 2) {
        event.preventDefault();
        renderer.setAiming(false);
    }
});
canvas.addEventListener("contextmenu", (event) => event.preventDefault());
document.addEventListener("mousemove", (event) => {
    if (document.pointerLockElement !== canvas) {
        return;
    }
    yaw -= event.movementX * 0.002;
    pitch = Math.max(-1.45, Math.min(1.45, pitch - event.movementY * 0.002));
    renderer.camera.rotation.set(pitch, yaw, 0);
});

function updateMovement(deltaSeconds) {
    // W follows the camera forward vector; S reverses it. A/D use camera right.
    movement.forward = Number(keys.has("KeyW")) - Number(keys.has("KeyS"));
    movement.strafe = Number(keys.has("KeyD")) - Number(keys.has("KeyA"));
    if (stance !== "PRONE") {
        stance = keys.has("KeyC") ? "CROUCHING" : "STANDING";
    }
    if (keys.has("Space") && onGround && stance === "STANDING") {
        sounds.unlock();
        sounds.jump();
        verticalVelocity = 6.5;
        onGround = false;
    }
    verticalVelocity -= 18 * deltaSeconds;
    const eyeHeight = stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7;
    renderer.camera.position.y += verticalVelocity * deltaSeconds;
    if (renderer.camera.position.y <= eyeHeight) {
        renderer.camera.position.y = eyeHeight;
        verticalVelocity = 0;
        onGround = true;
    }
    const speed = (stance === "PRONE" ? 2.5 : stance === "CROUCHING" ? 4 : 7) * deltaSeconds;
    const length = Math.hypot(movement.forward, movement.strafe) || 1;
    const forward = movement.forward / length;
    const strafe = movement.strafe / length;
    // Three.js camera looks down local -Z. Apply the same yaw transform to
    // movement so W/S follow the crosshair direction and A/D follow camera right.
    const forwardX = -Math.sin(yaw);
    const forwardZ = -Math.cos(yaw);
    const rightX = Math.cos(yaw);
    const rightZ = -Math.sin(yaw);
    const nextX = renderer.camera.position.x
        + (forward * forwardX + strafe * rightX) * speed;
    const nextZ = renderer.camera.position.z
        + (forward * forwardZ + strafe * rightZ) * speed;
    const feetY = renderer.camera.position.y - (stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7);
    if (renderer.canMoveTo(nextX, renderer.camera.position.z, feetY)) {
        renderer.camera.position.x = nextX;
    }
    if (renderer.canMoveTo(renderer.camera.position.x, nextZ, feetY)) {
        renderer.camera.position.z = nextZ;
    }
    renderer.updateLocalPlayer(
        {
            x: renderer.camera.position.x,
            y: renderer.camera.position.y - 0.9,
            z: renderer.camera.position.z
        },
        { x: 0, y: yaw, z: 0 },
        stance
    );
}

function publishLocalState() {
    socket.publishState(roomCode, {
        playerId,
        position: {
            x: renderer.camera.position.x,
            y: renderer.camera.position.y,
            z: renderer.camera.position.z
        },
        rotation: { x: pitch, y: yaw, z: 0 },
        health: 100,
        currentWeapon: weapon,
        stance
    });
}

function formatKill(event) {
    return `${event.killerName} killed ${event.victimName} with ${event.weapon.replaceAll("_", " ")}`;
}

async function startGame() {
    document.querySelector("#lobby-screen").hidden = true;
    await renderer.loadMap(selectedMap);
    await renderer.loadPlayerAssets();
    renderer.spawnLocalPlayer(weapon);
    statusElement.textContent = "Arena ready";
    roomStatusElement.textContent = `Room: ${roomCode}`;
    socket.connect(
        roomCode,
        {
            playerId,
            displayName,
            position: { x: 0, y: 1.7, z: 8 },
            rotation: { x: 0, y: 0, z: 0 },
            health: 100,
            currentWeapon: weapon,
            stance
        },
        (state) => {
            renderer.updateRemotePlayers(state.players, playerId);
            matchTimerElement.textContent = state.matchStatus === "RUNNING"
                ? `Time: ${Math.floor(state.remainingSeconds / 60)}:${String(state.remainingSeconds % 60).padStart(2, "0")}`
                : state.matchStatus === "OVER" ? "Match over" : "Waiting for players";
        },
        (message) => { statusElement.textContent = message; },
        (event) => {
            sounds.hit();
            const entry = document.createElement("div");
            entry.className = "kill-entry";
            entry.textContent = formatKill(event);
            killFeedElement.prepend(entry);
            window.setTimeout(() => entry.remove(), 8000);
        },
        (event) => {
            sounds.kill();
            matchOverElement.hidden = false;
            matchOverElement.textContent = event.winnerName
                ? `Match Over — ${event.winnerName} wins with ${event.verifiedKills} kills`
                : "Match Over — no winner";
        },
        (event) => {
            const entry = document.createElement("div");
            entry.className = "kill-entry";
            entry.textContent = `${event.displayName} left the match`;
            killFeedElement.prepend(entry);
            window.setTimeout(() => entry.remove(), 5000);
        }
    );
    window.addEventListener("beforeunload", () => socket.disconnect(roomCode, playerId));
    requestAnimationFrame(renderFrame);
}

function renderFrame(now) {
    const deltaSeconds = Math.min((now - lastTime) / 1000, 0.1);
    lastTime = now;
    updateMovement(deltaSeconds);
    renderer.interpolateRemotePlayers(deltaSeconds);
    if (now - lastNetworkUpdate >= 50) {
        publishLocalState();
        lastNetworkUpdate = now;
    }
    renderer.render();
    requestAnimationFrame(renderFrame);
}

async function submitLobby(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const requestedRoom = String(form.get("roomCode") || "").trim().toUpperCase();
    displayName = String(form.get("displayName")).trim();
    weapon = String(form.get("weapon"));
    const lobbyError = document.querySelector("#lobby-error");
    lobbyError.textContent = "";
    try {
        const response = requestedRoom
            ? await fetch(`${API_BASE_URL}/api/lobbies/${requestedRoom}/join`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ displayName })
            })
            : await fetch(`${API_BASE_URL}/api/lobbies`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    displayName,
                    map: String(form.get("map")),
                    playerLimit: Number(form.get("playerLimit"))
                })
            });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Could not create or join the room");
        }
        const lobby = await response.json();
        roomCode = lobby.roomCode;
        selectedMap = lobby.map;
        window.history.replaceState(
            {},
            "",
            `/?room=${roomCode}&name=${encodeURIComponent(displayName)}&weapon=${weapon}&map=${selectedMap}`
        );
        await startGame();
    } catch (error) {
        lobbyError.textContent = error.message;
    }
}

document.querySelector("#lobby-form").addEventListener("submit", submitLobby);

if (roomCode && displayName) {
    startGame().catch((error) => {
        console.error(error);
        statusElement.textContent = "Unable to initialize the arena";
    });
}
