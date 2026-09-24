import { ArenaStrikeRenderer } from "./game/renderer.js";
import { ArenaStrikeSocket } from "./network/stomp-client.js";
import { API_BASE_URL } from "./config.js?v=2";
import { ArenaStrikeSoundManager } from "./audio/sound-manager.js";

const canvas = document.querySelector("#game-canvas");
const statusElement = document.querySelector("#connection-status");
const roomStatusElement = document.querySelector("#room-status");
const matchTimerElement = document.querySelector("#match-timer");
const killFeedElement = document.querySelector("#kill-feed");
const matchOverElement = document.querySelector("#match-over");
const weaponNameElement = document.querySelector("#weapon-name");
const currentAmmoElement = document.querySelector("#current-ammo");
const reserveAmmoElement = document.querySelector("#reserve-ammo");
const healthLabelElement = document.querySelector("#health-label");
const healthBarElement = document.querySelector("#health-bar");
const renderer = new ArenaStrikeRenderer(canvas);
const socket = new ArenaStrikeSocket();
const sounds = new ArenaStrikeSoundManager();
const keys = new Set();
const query = new URLSearchParams(window.location.search);
let roomCode = query.get("room")?.toUpperCase() || "";
// playerId is always assigned from the server's DB-generated value after
// createLobby or joinLobby succeeds. It must never be a client-generated
// random number because StatsService looks up real database rows by this ID.
let playerId = null;
let displayName = query.get("name") || "";
let weapon = query.get("weapon") || "ASSAULT_RIFLE";
let selectedMap = query.get("map") || "MAP_1";
const movement = { forward: 0, strafe: 0 };
const playerKills = new Map();
let yaw = 0;
let pitch = 0;
let lastTime = performance.now();
let lastNetworkUpdate = 0;
let playerVelocityX = 0;
let playerVelocityZ = 0;
let verticalVelocity = 0;
let onGround = true;
let stance = "STANDING";
let inputEnabled = true;
let gameRunning = false;
let animationFrameId = null;
let teardownComplete = false;
let unloadHandler = null;
let lastProcessedServerTick = -1;
const MAGAZINE_SIZES = {
    PISTOL: 12,
    ASSAULT_RIFLE: 30,
    SMG: 25,
    SHOTGUN: 2,
    BOLT_ACTION_RIFLE: 5
};
let currentAmmo = MAGAZINE_SIZES[weapon] || 30;
let playerState = "IDLE";
let isReloading = false;         // kept in sync by the AMMO_STATE server event
let reserveAmmo = (MAGAZINE_SIZES[weapon] || 30) * 3;

function updateAmmoDisplay() {
    weaponNameElement.textContent = weapon.replaceAll("_", " ");
    currentAmmoElement.textContent = String(currentAmmo);
    reserveAmmoElement.textContent = String(reserveAmmo);
}
updateAmmoDisplay();

function updateTimer(remainingSeconds) {
    if (remainingSeconds <= 0) {
        matchTimerElement.textContent = `00:00`;
        matchTimerElement.className = "timer-danger";
        return;
    }
    const minutes = String(Math.floor(remainingSeconds / 60)).padStart(2, "0");
    const seconds = String(remainingSeconds % 60).padStart(2, "0");
    matchTimerElement.textContent = `${minutes}:${seconds}`;
    
    if (remainingSeconds < 60) {
        matchTimerElement.className = "timer-danger";
    } else {
        matchTimerElement.className = "timer-normal";
    }
}

function updateLiveScoreboard(players) {
    const scoreboardBody = document.querySelector("#mini-scoreboard-body");
    
    const rankedPlayers = players.map(p => {
        return {
            name: p.displayName,
            playerId: p.playerId,
            kills: playerKills.get(p.displayName) || 0
        };
    });
    
    rankedPlayers.sort((a, b) => b.kills - a.kills);
    
    const localPlayer = rankedPlayers.find(p => String(p.playerId) === String(playerId));
    if (localPlayer) {
        const localScoreEl = document.querySelector("#local-score");
        if (localScoreEl) localScoreEl.textContent = localPlayer.kills;
    }
    
    const opponents = rankedPlayers.filter(p => String(p.playerId) !== String(playerId));
    if (opponents.length > 0) {
        const oppScoreEl = document.querySelector("#opponent-score");
        if (oppScoreEl) oppScoreEl.textContent = opponents[0].kills;
    }
    
    if (scoreboardBody) {
        const top3 = rankedPlayers.slice(0, 3);
        scoreboardBody.innerHTML = top3.map(p => `
            <tr>
                <td>${p.name}</td>
                <td>${p.kills}</td>
            </tr>
        `).join("");
    }
}

function stopClientLoops() {
    gameRunning = false;
    keys.clear();
    movement.forward = 0;
    movement.strafe = 0;
    playerVelocityX = 0;
    playerVelocityZ = 0;
    console.trace("Who called WebSocket close?");
    verticalVelocity = 0;
    onGround = true;
    if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
    }
    renderer.resetEntities();
    sounds.stop();
}

function handleBeforeUnload() {
    if (teardownComplete) {
        return;
    }
    teardownComplete = true;
    detachInputListeners();
    stopClientLoops();
    socket.disconnect(roomCode, playerId);
}

function onKeyDown(event) {
    if (!inputEnabled) return;
    if (["KeyW", "KeyA", "KeyS", "KeyD", "Space", "KeyR"].includes(event.code)) {
        event.preventDefault();
    }
    keys.add(event.code);
    if (event.code === "KeyZ") {
        stance = stance === "PRONE" ? "STANDING" : "PRONE";
    }
    if (event.code === "KeyH") {
        document.querySelector("#controls-hint").classList.toggle("hidden");
    }
    if (event.code === "KeyR") {
        triggerReload();
    }
}
function onKeyUp(event) {
    if (!inputEnabled) return;
    if (["KeyW", "KeyA", "KeyS", "KeyD", "Space"].includes(event.code)) {
        event.preventDefault();
    }
    keys.delete(event.code);
}
function onWindowBlur() { keys.clear(); }
function onCanvasClick() {
    if (!inputEnabled) return;
    sounds.unlock();
    if (document.pointerLockElement !== canvas) {
        canvas.requestPointerLock();
    }
}

function triggerReload() {
    if (playerState === "RELOADING" || currentAmmo >= MAGAZINE_SIZES[weapon] || reserveAmmo <= 0) return;
    
    playerState = "RELOADING";
    renderer.setAiming(false); // cancel ADS
    document.querySelector("#crosshair").hidden = false;
    renderer.playReloadAnimation();
    
    if (socket.client?.connected) {
        socket.reload(roomCode, { weaponId: weapon });
    }
    
    window.setTimeout(() => {
        const needed = MAGAZINE_SIZES[weapon] - currentAmmo;
        const taken = Math.min(needed, reserveAmmo);
        currentAmmo += taken;
        reserveAmmo -= taken;
        playerState = "IDLE";
        updateAmmoDisplay();
    }, 2500);
}
function onMouseDown(event) {
    if (!inputEnabled) return;
    if (playerState === "RELOADING") return; // Prevent action while reloading
    if (event.button === 2) {
        event.preventDefault();
        sounds.unlock();
        renderer.setAiming(true);
        document.querySelector("#crosshair").hidden = true;
        return;
    }
    if (event.button !== 0 || document.pointerLockElement !== canvas) {
        return;
    }
    if (currentAmmo <= 0) {
        if (reserveAmmo > 0) {
            triggerReload();
        } else {
            sounds.empty();
        }
        return;
    }

    currentAmmo--;
    updateAmmoDisplay();

    const spreadMultiplier = renderer.aiming ? 0.4 : 1.0;
    const spreadPitch = (Math.random() - 0.5) * 0.04 * spreadMultiplier;
    const spreadYaw = (Math.random() - 0.5) * 0.04 * spreadMultiplier;
    const actualPitch = pitch + spreadPitch;
    const actualYaw = yaw + spreadYaw;

    renderer.fireWeapon(actualPitch, actualYaw);
    sounds.fire();
    if (socket.client?.connected) {
        socket.shoot(roomCode, {
            action: "FIRE",
            timestamp: Date.now(),
            angle: actualPitch,
            weaponId: weapon
        });
    }
}
function onMouseUp(event) {
    if (!inputEnabled) return;
    if (event.button === 2) {
        event.preventDefault();
        renderer.setAiming(false);
        document.querySelector("#crosshair").hidden = false;
    }
}
function onContextMenu(event) { event.preventDefault(); }
function onMouseMove(event) {
    if (!inputEnabled) return;
    if (document.pointerLockElement !== canvas) {
        return;
    }
    yaw -= event.movementX * 0.002;
    pitch = Math.max(-1.45, Math.min(1.45, pitch - event.movementY * 0.002));
    renderer.camera.rotation.set(pitch, yaw, 0);
}
function detachInputListeners() {
    inputEnabled = false;
    keys.clear();
    window.removeEventListener("keydown", onKeyDown);
    window.removeEventListener("keyup", onKeyUp);
    window.removeEventListener("blur", onWindowBlur);
    canvas.removeEventListener("click", onCanvasClick);
    canvas.removeEventListener("mousedown", onMouseDown);
    canvas.removeEventListener("mouseup", onMouseUp);
    canvas.removeEventListener("contextmenu", onContextMenu);
    window.removeEventListener("mousemove", onMouseMove);
    if (document.pointerLockElement === canvas) document.exitPointerLock();
}
window.addEventListener("keydown", onKeyDown);
window.addEventListener("keyup", onKeyUp);
window.addEventListener("blur", onWindowBlur);
canvas.addEventListener("click", onCanvasClick);
canvas.addEventListener("mousedown", onMouseDown);
canvas.addEventListener("mouseup", onMouseUp);
canvas.addEventListener("contextmenu", onContextMenu);
window.addEventListener("mousemove", onMouseMove);

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
    if (!onGround) {
        stance = "JUMPING";
    } else if (stance === "JUMPING") {
        stance = "STANDING";
    }
    verticalVelocity -= 18 * deltaSeconds;
    const eyeHeight = stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7;
    renderer.camera.position.y += verticalVelocity * deltaSeconds;
    if (renderer.camera.position.y <= eyeHeight) {
        renderer.camera.position.y = eyeHeight;
        verticalVelocity = 0;
        onGround = true;
    }
    const targetSpeed = (stance === "PRONE" ? 2.5 : stance === "CROUCHING" ? 4 : 7);
    const length = Math.hypot(movement.forward, movement.strafe) || 1;
    const forward = movement.forward / length;
    const strafe = movement.strafe / length;
    // Three.js camera looks down local -Z. Apply the same yaw transform to
    // movement so W/S follow the crosshair direction and A/D follow camera right.
    const forwardX = -Math.sin(yaw);
    const forwardZ = -Math.cos(yaw);
    const rightX = Math.cos(yaw);
    const rightZ = -Math.sin(yaw);
    
    const targetVelX = (forward * forwardX + strafe * rightX) * targetSpeed;
    const targetVelZ = (forward * forwardZ + strafe * rightZ) * targetSpeed;

    const acceleration = (movement.forward !== 0 || movement.strafe !== 0) ? (onGround ? 10 : 3) : (onGround ? 8 : 1);
    
    playerVelocityX += (targetVelX - playerVelocityX) * acceleration * deltaSeconds;
    playerVelocityZ += (targetVelZ - playerVelocityZ) * acceleration * deltaSeconds;

    const nextX = renderer.camera.position.x + playerVelocityX * deltaSeconds;
    const nextZ = renderer.camera.position.z + playerVelocityZ * deltaSeconds;

    const feetY = renderer.camera.position.y - (stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7);
    if (renderer.canMoveTo(nextX, renderer.camera.position.z, feetY)) {
        renderer.camera.position.x = nextX;
    } else {
        playerVelocityX = 0;
    }
    if (renderer.canMoveTo(renderer.camera.position.x, nextZ, feetY)) {
        renderer.camera.position.z = nextZ;
    } else {
        playerVelocityZ = 0;
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
    const feetY = renderer.camera.position.y - (stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7);
    socket.publishInput(roomCode, {
        playerId,
        x: renderer.camera.position.x,
        y: feetY,
        z: renderer.camera.position.z,
        velocityX: playerVelocityX,
        velocityY: verticalVelocity,
        state: stance,
        isJumping: keys.has("Space"),
        rotation: { x: pitch, y: yaw, z: 0 },
        currentWeapon: weapon
    });
}

function formatKill(event) {
    return `${event.killerName} killed ${event.victimName} with ${event.weapon.replaceAll("_", " ")}`;
}

async function startGame() {
    gameRunning = true;
    teardownComplete = false;
    const lobbyScreen = document.querySelector("#lobby-screen");
    const lobbyWaitingScreen = document.querySelector("#lobby-waiting-screen");
    const gameShell = document.querySelector("#game-shell");
    
    if (lobbyScreen) lobbyScreen.hidden = true;
    if (lobbyWaitingScreen) lobbyWaitingScreen.style.display = "flex";
    if (gameShell) gameShell.hidden = true;

    const startMatchBtn = document.getElementById("start-match-btn");
    if (startMatchBtn) {
        startMatchBtn.onclick = () => {
            socket.startMatch(roomCode);
        };
    }

    await renderer.loadMap(selectedMap);


    await renderer.loadPlayerAssets();
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
            state: stance
        },
        (state) => {
            if (!state || typeof state.serverTick !== 'number') return;
            if (state.serverTick <= lastProcessedServerTick) return;
            lastProcessedServerTick = state.serverTick;
            
            try {
                if (Array.isArray(state.players)) {
                    renderer.updateRemotePlayers(state.players, playerId, state.serverTick);
                    const local = state.players.find((player) => player.playerId === playerId);
                    if (local) {
                        updateHealth(local.health);
                    }
                    updateLiveScoreboard(state.players);
                }
            } catch (error) {
                console.error("Failed to parse remote players:", error, state.players);
            }
            if (state.roomState !== "ACTIVE") {
                matchTimerElement.textContent = state.roomState === "FINISHED" ? "Match over" : "Waiting for players";
            } else if (state.remainingSeconds) {
                updateTimer(state.remainingSeconds);
            }
        },
        (message) => { statusElement.textContent = message; },
        (event) => {
            sounds.hit();
            const entry = document.createElement("div");
            entry.className = "kill-entry";
            entry.textContent = formatKill(event);
            killFeedElement.prepend(entry);
            window.setTimeout(() => entry.remove(), 8000);
            
            if (event.killerName && event.killerName !== event.victimName) {
                playerKills.set(event.killerName, (playerKills.get(event.killerName) || 0) + 1);
            }
        },
        (event) => {
            handleMatchOver(event);
        },
        (event) => {
            const entry = document.createElement("div");
            entry.className = "kill-entry";
            entry.textContent = `${event.displayName} left the match`;
            killFeedElement.prepend(entry);
            window.setTimeout(() => entry.remove(), 5000);
        },
        (event) => {
            if (event.hit) {
                renderer.showHitMarker(event.hit);
            }
            if (event.event === "SHOT_VERIFIED" && String(event.attackerId) !== String(playerId)) {
                const remote = renderer.remotePlayers.get(Number(event.attackerId));
                if (remote && remote.mesh) {
                    sounds.fire(remote.mesh.position);
                }
            }
        },
        (event) => {
            if (event.remainingSeconds !== undefined) {
                updateTimer(event.remainingSeconds);
            }
        },
        (event) => {
            currentAmmo = event.currentAmmo;
            isReloading = event.reloading;
            updateAmmoDisplay();
        },
        (respawnEvent) => {
            if (String(respawnEvent.playerId) === String(playerId)) {
                renderer.camera.position.set(respawnEvent.position.x, respawnEvent.position.y, respawnEvent.position.z);
                updateHealth(100);
            }
        },
        handleUnexpectedDisconnect,
        (lobbyState) => {
            document.getElementById("lobby-player-count").innerText = lobbyState.currentPlayers;
            document.getElementById("lobby-max-players").innerText = lobbyState.maxPlayers;
        },
        () => {
            const waitingScreen = document.querySelector("#lobby-waiting-screen");
            const gameShell = document.querySelector("#game-shell");
            if (waitingScreen) waitingScreen.style.display = "none";
            if (gameShell) gameShell.hidden = false;
            renderer.spawnLocalPlayer(weapon);
            animationFrameId = requestAnimationFrame(renderFrame);
            
            // Request Pointer Lock for the FPS camera
            const gameCanvas = document.querySelector("#game-canvas");
            if (gameCanvas) {
                try {
                    gameCanvas.requestPointerLock();
                } catch (e) {
                    console.warn("Pointer lock requires a user gesture", e);
                }
            }
        }
    );
    /*if (unloadHandler) {
        document.removeEventListener("visibilitychange", unloadHandler);
    }
    unloadHandler = () => {
        if (document.visibilityState === 'hidden') {
            handleBeforeUnload();
        }
    };
    document.addEventListener("visibilitychange", unloadHandler);*/
}

function updateHealth(health) {
    const value = Math.max(0, Math.min(100, Number(health) || 0));
    const valText = document.querySelector("#health-value-text");
    if (valText) valText.textContent = String(value);
    
    healthBarElement.setAttribute("aria-valuenow", String(value));
    healthBarElement.firstElementChild.style.width = `${value}%`;
    
    if (value <= 30) {
        healthBarElement.classList.add("health-critical");
    } else {
        healthBarElement.classList.remove("health-critical");
    }
}

function renderFrame(now) {
    if (!gameRunning) return;
    try {
        const deltaSeconds = Math.min((now - lastTime) / 1000, 0.1);
        lastTime = now;
        updateMovement(deltaSeconds);
        renderer.interpolateRemotePlayers(now, deltaSeconds);
        sounds.updateListener(renderer.camera.position, { x: pitch, y: yaw, z: 0 });
        if (now - lastNetworkUpdate >= 50) {
            publishLocalState();
            lastNetworkUpdate = now;
        }
        renderer.render(deltaSeconds);
    } catch (err) {
        console.error("[THREE.js Error] renderFrame crashed:", err);
    }
    animationFrameId = requestAnimationFrame(renderFrame);
}

function handleMatchOver(event) {
    if (teardownComplete) return;
    teardownComplete = true;
    detachInputListeners();
    stopClientLoops();
    socket.disconnect(roomCode, playerId);
    sounds.kill();
    matchOverElement.hidden = false;

    const isVictory = event.winnerName === displayName;
    const bannerText = isVictory ? "VICTORY" : event.winnerName ? "DEFEAT" : "MATCH COMPLETE";
    
    let rows = "";
    (event.results || []).forEach((r, idx) => {
        const kd = r.deaths > 0 ? (r.kills / r.deaths).toFixed(2) : r.kills.toFixed(2);
        rows += `<tr>
            <td>${idx + 1}</td>
            <td>${r.displayName}</td>
            <td>${r.kills}</td>
            <td>${r.deaths}</td>
            <td>${kd}</td>
            <td>${r.headshotKills || 0}</td>
            <td>${r.damageDealt || 0}</td>
        </tr>`;
    });

    matchOverElement.innerHTML = `
        <div class="match-over-modal">
            <h1 class="match-banner ${isVictory ? 'victory' : 'defeat'}">${bannerText}</h1>
            <table class="leaderboard-table">
                <thead>
                    <tr><th>Rank</th><th>Operator</th><th>Kills</th><th>Deaths</th><th>K/D</th><th>Headshots</th><th>DMG Dealt</th></tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
            <div class="match-actions">
                <button id="btn-return-lobby" class="btn-primary">RETURN TO LOBBY</button>
                <button id="btn-play-again" class="btn-secondary">PLAY AGAIN</button>
            </div>
        </div>
    `;

    document.getElementById("btn-return-lobby").addEventListener("click", () => navigateToLobby(false));
    document.getElementById("btn-play-again").addEventListener("click", () => navigateToLobby(true));
}

function navigateToLobby(requeue) {
    renderer.dispose();
    matchOverElement.hidden = true;
    matchOverElement.innerHTML = "";
    document.querySelector("#lobby-screen").hidden = false;
    const gameShell = document.querySelector("#game-shell");
    if (gameShell) gameShell.hidden = true;
    killFeedElement.replaceChildren();
    if (requeue) {
        document.querySelector("#lobby-form").querySelector("button[type='submit']").click();
    }
}

function handleUnexpectedDisconnect() {
    if (!gameRunning || teardownComplete) return;
    teardownComplete = true;
    detachInputListeners();
    stopClientLoops();
    renderer.dispose();
    killFeedElement.replaceChildren();
    matchOverElement.hidden = true;
    matchTimerElement.textContent = "Disconnected";
    statusElement.textContent = "Connection lost. Return to the lobby.";
    document.querySelector("#lobby-screen").hidden = false;
    const gameShell = document.querySelector("#game-shell");
    if (gameShell) gameShell.hidden = true;
    document.querySelector("#lobby-error").textContent =
        "Disconnected from server. Please join or create a new room.";
    window.history.replaceState({}, "", "/");
}

async function submitLobby(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const requestedRoom = String(form.get("roomCode") || "").trim().toUpperCase();
    displayName = String(form.get("displayName")).trim();
    weapon = String(form.get("weapon"));
    
    const lobbyError = document.querySelector("#lobby-error");
    const errorMessage = lobbyError.querySelector(".error-message");
    const submitBtn = document.querySelector("#btn-submit-lobby");
    const btnText = submitBtn.querySelector(".btn-text");
    const btnSpinner = submitBtn.querySelector(".btn-spinner");
    
    // Reset UI state
    lobbyError.hidden = true;
    errorMessage.textContent = "";
    submitBtn.disabled = true;
    btnText.textContent = "Connecting...";
    btnSpinner.hidden = false;

    // Dismiss error handler
    const dismissBtn = lobbyError.querySelector(".error-dismiss");
    if (dismissBtn) {
        dismissBtn.onclick = () => lobbyError.hidden = true;
    }

    try {
        const password = String(form.get("password") || "");
        let jwtToken = null;
        
        const authRes = await fetch(`${API_BASE_URL}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: displayName, password: password })
        });
        
        if (authRes.ok) {
            const authData = await authRes.json();
            jwtToken = authData.token;
            displayName = authData.username; // update in case of guest UUID fallback
            sessionStorage.setItem("arena_jwt", jwtToken);
        } else {
            throw new Error("Invalid password or authentication failed");
        }

        const response = requestedRoom
            ? await fetch(`${API_BASE_URL}/api/lobby/${requestedRoom}/join`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    ...(jwtToken ? { "Authorization": `Bearer ${jwtToken}` } : {})
                },
                body: JSON.stringify({ displayName })
            })
            : await fetch(`${API_BASE_URL}/api/lobby/create`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    ...(jwtToken ? { "Authorization": `Bearer ${jwtToken}` } : {})
                },
                body: JSON.stringify({
                    roomName: String(form.get("roomName")) || "Arena Match",
                    displayName,
                    map: String(form.get("map")),
                    playerLimit: Number(form.get("playerLimit"))
                })
            });
            
        if (!response.ok) {
            let errorMsg = `Server error: ${response.status} ${response.statusText}`;
            try {
                const errorPayload = await response.json();
                if (errorPayload && errorPayload.message) {
                    errorMsg = errorPayload.message;
                } else if (errorPayload && errorPayload.error) {
                    errorMsg = errorPayload.error;
                }
            } catch (parseError) {
                // If it's not JSON, it might be an HTML error page or empty
                const textPayload = await response.text().catch(() => "");
                if (textPayload) errorMsg = textPayload.substring(0, 100);
            }
            throw new Error(errorMsg);
        }
        
        const lobby = await response.json();
        
        if (!lobby.myPlayerId) {
            throw new Error("Server did not return a player ID. Cannot start game.");
        }
        
        playerId = lobby.myPlayerId;
        roomCode = lobby.roomCode;
        selectedMap = lobby.map;
        
        document.getElementById("lobby-room-code").textContent = roomCode;
        document.getElementById("lobby-player-count").innerText = lobby.currentPlayers;
        document.getElementById("lobby-max-players").innerText = lobby.maxPlayers;
        
        if (String(lobby.hostId) === String(playerId)) {
            document.getElementById("start-match-btn").style.display = 'block';
        } else {
            document.getElementById("start-match-btn").style.display = 'none';
        }
        
        window.history.replaceState(
            {},
            "",
            `/?room=${roomCode}&name=${encodeURIComponent(displayName)}&weapon=${weapon}&map=${selectedMap}`
        );
        
        await startGame();
    } catch (error) {
        console.error("Lobby API Error:", error);
        
        // Handle "Failed to fetch" which is a TypeError when CORS fails or server is offline
        if (error instanceof TypeError && error.message === "Failed to fetch") {
            errorMessage.textContent = "Network error: Failed to reach the server. Is the backend running on " + API_BASE_URL + "?";
        } else {
            errorMessage.textContent = error.message;
        }
        lobbyError.hidden = false;
    } finally {
        submitBtn.disabled = false;
        btnText.textContent = "Create / Join Room";
        btnSpinner.hidden = true;
    }
}

document.querySelector("#lobby-form").addEventListener("submit", submitLobby);

if (roomCode && displayName) {
    startGame().catch((error) => {
        console.error(error);
        statusElement.textContent = "Unable to initialize the arena";
    });
}
