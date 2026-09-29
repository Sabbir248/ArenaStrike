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
let accountNickname = null;
let authToken = sessionStorage.getItem("arena_jwt") || "";
let weapon = query.get("weapon") || "ASSAULT_RIFLE";
let selectedMap = query.get("map") || "MAP_1";
const movement = { forward: 0, strafe: 0 };
const playerKills = new Map();
let yaw = 0;
let pitch = 0;
let currentRecoilPitch = 0;
let targetRecoilPitch = 0;
let currentRecoilYaw = 0;
let targetRecoilYaw = 0;
let shakeTime = 0;
let shakeIntensity = 0;
let currentHealthVal = 100;
let lastTime = performance.now();
let lastNetworkUpdate = 0;
let playerVelocityX = 0;
let playerVelocityZ = 0;
let verticalVelocity = 0;
let onGround = true;
let stance = "STANDING";
let currentEyeHeight = 1.7;
let inputEnabled = true;
let localSpawnSynced = false;
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

const WEAPON_STATS = {
    ASSAULT_RIFLE: { cooldown: 0.12, recoilPitch: 0.05, recoilYaw: 0.02, fullAuto: true },
    SMG: { cooldown: 0.075, recoilPitch: 0.03, recoilYaw: 0.015, fullAuto: true },
    PISTOL: { cooldown: 0.15, recoilPitch: 0.04, recoilYaw: 0.01, fullAuto: false },
    SHOTGUN: { cooldown: 1.2, recoilPitch: 0.12, recoilYaw: 0.03, fullAuto: false },
    BOLT_ACTION_RIFLE: { cooldown: 1.5, recoilPitch: 0.10, recoilYaw: 0.02, fullAuto: false }
};

let currentAmmo = MAGAZINE_SIZES[weapon] || 30;
let isFiring = false;
let lastFireTime = 0;
let playerState = "IDLE";
let isReloading = false;         // kept in sync by the AMMO_STATE server event
let reserveAmmo = (MAGAZINE_SIZES[weapon] || 30) * 3;

function updateAmmoDisplay() {
    weaponNameElement.textContent = weapon.replaceAll("_", " ");
    currentAmmoElement.textContent = String(currentAmmo);
    reserveAmmoElement.textContent = String(reserveAmmo);
}
updateAmmoDisplay();

function showLobbyMessage(message) {
    const lobbyError = document.querySelector("#lobby-error");
    lobbyError.querySelector(".error-message").textContent = message;
    lobbyError.hidden = false;
}
document.querySelector("#lobby-error .error-dismiss").addEventListener("click", () => {
    document.querySelector("#lobby-error").hidden = true;
});

function completeLogin(name) {
    const normalizedName = String(name || "").trim().slice(0, 32);
    if (!normalizedName) {
        showLobbyMessage("We couldn't read a name from that profile. Please try again.");
        return false;
    }

    displayName = normalizedName;
    document.querySelector("#display-name").value = displayName;
    document.querySelector("#welcome-name").textContent = displayName;
    document.querySelector("#login-buttons").hidden = true;
    document.querySelector("#guest-entry").hidden = true;
    document.querySelector("#welcome-panel").hidden = false;
    document.querySelector("#lobby-options").hidden = false;
    document.querySelector("#lobby-submit-section").hidden = false;
    document.querySelector("#btn-submit-lobby").disabled = !accountNickname || !authToken;
    document.querySelector("#lobby-error").hidden = true;
    return true;
}

function suggestNickname(name) {
    const cleaned = String(name || "").normalize("NFKC").replace(/[^\p{L}\p{N}_ ]/gu, "").trim().replace(/\s+/g, "_").slice(0, 24);
    return cleaned.length >= 3 ? cleaned : `Player_${Math.floor(1000 + Math.random() * 9000)}`;
}

function setAuthenticatedSession(account, fallbackName) {
    authToken = account.token;
    playerId = account.playerId;
    accountNickname = account.nickname || null;
    sessionStorage.setItem("arena_jwt", authToken);
    const preferredName = accountNickname || account.profileName || fallbackName || account.username;
    completeLogin(preferredName);
    return refreshSavedNickname(fallbackName || account.profileName || account.username);
}

async function refreshSavedNickname(fallbackName) {
    if (!authToken) return;
    try {
        const response = await fetch(`${API_BASE_URL}/api/settings/nickname`, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        if (!response.ok) throw new Error("Unable to load your saved nickname.");
        const profile = await response.json();
        accountNickname = profile.nickname || null;
        if (accountNickname) {
            displayName = accountNickname;
            document.querySelector("#display-name").value = accountNickname;
            document.querySelector("#welcome-name").textContent = accountNickname;
            document.querySelector("#btn-submit-lobby").disabled = false;
            document.querySelector("#settings-modal").hidden = true;
        } else {
            document.querySelector("#btn-submit-lobby").disabled = true;
            document.querySelector("#nickname-input").value = suggestNickname(fallbackName);
            openNicknameSettings();
        }
    } catch (error) {
        showLobbyMessage(error.message || "Unable to load your saved nickname.");
    }
}

async function authenticateWithOAuth(provider, credential, fallbackName) {
    const response = await fetch(`${API_BASE_URL}/auth/oauth`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ provider, credential })
    });
    if (!response.ok) {
        let message = "Social sign-in could not be verified by the game server.";
        try {
            const error = await response.json();
            message = error.detail || error.message || message;
        } catch { /* Keep the friendly fallback for non-JSON errors. */ }
        throw new Error(message);
    }
    await setAuthenticatedSession(await response.json(), fallbackName);
}

function openNicknameSettings() {
    document.querySelector("#nickname-error").textContent = "";
    document.querySelector("#settings-modal").hidden = false;
    document.querySelector("#nickname-input").focus();
}

const GOOGLE_CLIENT_ID = "669840629062-homu57ddil3e8nj1eqlmniak356cgr0d.apps.googleusercontent.com";
let googleIdentityInitialized = false;

function handleGoogleCredentialResponse(response) {
    try {
        if (!response?.credential) throw new Error("Google did not return a credential.");
        // This decode is only used to populate the display name. The ID token
        // must be verified by the backend before it is trusted for auth.
        const encodedPayload = response.credential.split(".")[1];
        const base64Payload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
        const paddedPayload = base64Payload.padEnd(Math.ceil(base64Payload.length / 4) * 4, "=");
        const bytes = Uint8Array.from(atob(paddedPayload), (character) => character.charCodeAt(0));
        const profile = JSON.parse(new TextDecoder().decode(bytes));
        authenticateWithOAuth("google", response.credential, profile.name)
            .catch((error) => showLobbyMessage(error.message || "Google sign-in could not be completed."));
    } catch (error) {
        showLobbyMessage(error?.message || "Google sign-in could not be completed.");
    }
}

function initializeGoogleIdentity() {
    if (googleIdentityInitialized || !window.google?.accounts?.id) return;
    window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: handleGoogleCredentialResponse
    });
    googleIdentityInitialized = true;
}

const googleSdkScript = document.querySelector("#google-gsi-sdk");
googleSdkScript.addEventListener("load", initializeGoogleIdentity);
initializeGoogleIdentity();

function loginWithGoogle() {
    initializeGoogleIdentity();
    if (GOOGLE_CLIENT_ID === "YOUR_GOOGLE_CLIENT_ID_HERE") {
        showLobbyMessage("Add your Google OAuth client ID in main.js to enable Google sign-in.");
        return;
    }
    if (!googleIdentityInitialized) {
        showLobbyMessage("Google sign-in is still loading. Please try again shortly.");
        return;
    }
    window.google.accounts.id.prompt();
}

window.loginWithGoogle = loginWithGoogle;

document.querySelector("#btn-google-login").addEventListener("click", loginWithGoogle);
document.querySelector("#btn-guest-login").addEventListener("click", () => {
    const guestEntry = document.querySelector("#guest-entry");
    guestEntry.hidden = false;
    document.querySelector("#login-buttons").hidden = true;
    const guestName = document.querySelector("#guest-name");
    if (!guestName.value) guestName.value = `Guest_${Math.floor(1000 + Math.random() * 9000)}`;
    guestName.focus();
});
document.querySelector("#btn-guest-continue").addEventListener("click", async () => {
    const guestName = document.querySelector("#guest-name").value.trim();
    if (!guestName) {
        showLobbyMessage("Enter a guest name to continue.");
        document.querySelector("#guest-name").focus();
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: guestName })
        });
        if (!response.ok) throw new Error("Guest sign-in could not be completed.");
        const account = await response.json();
        await setAuthenticatedSession({ ...account, profileName: account.username }, guestName);
    } catch (error) {
        showLobbyMessage(error.message || "Guest sign-in could not be completed.");
    }
});
document.querySelector("#guest-name").addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault();
        document.querySelector("#btn-guest-continue").click();
    }
});
document.querySelector("#btn-change-login").addEventListener("click", () => {
    displayName = "";
    accountNickname = null;
    authToken = "";
    playerId = null;
    sessionStorage.removeItem("arena_jwt");
    document.querySelector("#display-name").value = "";
    document.querySelector("#btn-submit-lobby").disabled = true;
    document.querySelector("#welcome-panel").hidden = true;
    document.querySelector("#lobby-options").hidden = true;
    document.querySelector("#lobby-submit-section").hidden = true;
    document.querySelector("#guest-entry").hidden = true;
    document.querySelector("#login-buttons").hidden = false;
});

document.querySelector("#btn-open-settings").addEventListener("click", openNicknameSettings);
document.querySelector("#btn-close-settings").addEventListener("click", () => {
    if (accountNickname) document.querySelector("#settings-modal").hidden = true;
});
document.querySelector("#settings-modal").addEventListener("click", (event) => {
    if (event.target.id === "settings-modal" && accountNickname) {
        document.querySelector("#settings-modal").hidden = true;
    }
});
document.querySelector("#btn-save-nickname").addEventListener("click", async () => {
    const nicknameInput = document.querySelector("#nickname-input");
    const errorElement = document.querySelector("#nickname-error");
    const saveButton = document.querySelector("#btn-save-nickname");
    const nickname = nicknameInput.value.trim().replace(/\s+/g, " ");
    errorElement.textContent = "";
    if (nickname.length < 3 || nickname.length > 32) {
        errorElement.textContent = "Nickname must be between 3 and 32 characters.";
        nicknameInput.focus();
        return;
    }
    saveButton.disabled = true;
    try {
        const response = await fetch(`${API_BASE_URL}/api/settings/nickname`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${authToken}`
            },
            body: JSON.stringify({ nickname })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            if (response.status === 409 || payload.detail === "Nickname already taken") {
                throw new Error("Nickname already taken");
            }
            throw new Error(payload.detail || payload.message || "Nickname could not be saved.");
        }
        accountNickname = payload.nickname;
        displayName = accountNickname;
        document.querySelector("#display-name").value = displayName;
        document.querySelector("#welcome-name").textContent = displayName;
        document.querySelector("#btn-submit-lobby").disabled = false;
        document.querySelector("#settings-modal").hidden = true;
        const welcome = document.querySelector("#welcome-panel");
        welcome.classList.remove("nickname-saved");
        void welcome.offsetWidth;
        welcome.classList.add("nickname-saved");
    } catch (error) {
        errorElement.textContent = error.message;
    } finally {
        saveButton.disabled = false;
    }
});

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
    const teamABody = document.querySelector("#team-a-body");
    const teamBBody = document.querySelector("#team-b-body");
    
    const rankedPlayers = players.map(p => {
        return {
            name: p.displayName,
            playerId: p.playerId,
            team: p.team,
            kills: playerKills.get(p.displayName) || 0
        };
    });
    
    rankedPlayers.sort((a, b) => b.kills - a.kills);
    
    const localPlayer = rankedPlayers.find(p => String(p.playerId) === String(playerId));
    if (localPlayer) {
        const localScoreEl = document.querySelector("#local-score");
        if (localScoreEl) localScoreEl.textContent = localPlayer.kills;
    }
    
    const opponents = rankedPlayers.filter(p => String(p.playerId) !== String(playerId) && p.team !== localPlayer?.team);
    if (opponents.length > 0) {
        const oppScoreEl = document.querySelector("#opponent-score");
        if (oppScoreEl) oppScoreEl.textContent = opponents[0].kills;
    }
    
    const isTeamMode = rankedPlayers.some(p => p.team);
    const teamContainer = document.getElementById("team-scoreboard-container");
    const ffaContainer = document.getElementById("ffa-scoreboard-container");
    if (teamContainer) teamContainer.style.display = isTeamMode ? "flex" : "none";
    if (ffaContainer) ffaContainer.style.display = isTeamMode ? "none" : "block";

    if (isTeamMode) {
        if (teamABody) {
            const teamA = rankedPlayers.filter(p => p.team === "TEAM_A");
            teamABody.innerHTML = teamA.map(p => `
                <tr>
                    <td>${p.name}</td>
                    <td>${p.kills}</td>
                </tr>
            `).join("");
        }
        if (teamBBody) {
            const teamB = rankedPlayers.filter(p => p.team === "TEAM_B");
            teamBBody.innerHTML = teamB.map(p => `
                <tr>
                    <td>${p.name}</td>
                    <td>${p.kills}</td>
                </tr>
            `).join("");
        }
    } else {
        const ffaBody = document.getElementById("ffa-body");
        if (ffaBody) {
            ffaBody.innerHTML = rankedPlayers.map(p => `
                <tr>
                    <td>${p.name}</td>
                    <td>${p.kills}</td>
                </tr>
            `).join("");
        }
    }
}

function stopClientLoops() {
    gameRunning = false;
    isFiring = false;
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
    if (event.code === "KeyZ" || event.code === "KeyX") {
        stance = stance === "PRONE" ? "STANDING" : "PRONE";
    }
    if (event.code === "KeyC" || event.code === "ControlLeft" || event.code === "ControlRight") {
        stance = stance === "CROUCHING" ? "STANDING" : "CROUCHING";
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
function attemptFire() {
    if (playerState === "RELOADING") return;
    
    if (currentAmmo <= 0) {
        if (reserveAmmo > 0) {
            triggerReload();
        } else {
            sounds.empty();
        }
        return;
    }

    const now = performance.now() / 1000;
    const stats = WEAPON_STATS[weapon] || WEAPON_STATS["ASSAULT_RIFLE"];
    
    if (now - lastFireTime < stats.cooldown) {
        return;
    }
    
    lastFireTime = now;
    currentAmmo--;
    updateAmmoDisplay();

    const spreadMultiplier = renderer.aiming ? 0.4 : 1.0;
    const spreadPitch = (Math.random() - 0.5) * stats.recoilPitch * spreadMultiplier;
    const spreadYaw = (Math.random() - 0.5) * stats.recoilYaw * spreadMultiplier;
    
    targetRecoilPitch += stats.recoilPitch * spreadMultiplier;
    targetRecoilYaw += (Math.random() - 0.5) * stats.recoilYaw * spreadMultiplier;
    shakeTime = 0.15;
    shakeIntensity = Math.min(shakeIntensity + 0.015, 0.04);
    
    const actualPitch = pitch + currentRecoilPitch + spreadPitch;
    const actualYaw = yaw + currentRecoilYaw + spreadYaw;

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

function onMouseDown(event) {
    if (!inputEnabled) return;
    if (playerState === "RELOADING") return; // Prevent action while reloading
    if (event.button === 2) {
        event.preventDefault();
        sounds.unlock();
        renderer.setAiming(true);
        return;
    }
    if (event.button !== 0 || document.pointerLockElement !== canvas) {
        return;
    }
    
    // Set continuous fire state or fire once
    isFiring = true;
    attemptFire();
}
function onMouseUp(event) {
    if (!inputEnabled) return;
    if (event.button === 0) {
        isFiring = false;
    }
    if (event.button === 2) {
        event.preventDefault();
        renderer.setAiming(false);
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
function attachInputListeners() {
    inputEnabled = true;
    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("keyup", onKeyUp);
    window.addEventListener("blur", onWindowBlur);
    canvas.addEventListener("click", onCanvasClick);
    canvas.addEventListener("mousedown", onMouseDown);
    canvas.addEventListener("mouseup", onMouseUp);
    canvas.addEventListener("contextmenu", onContextMenu);
    window.addEventListener("mousemove", onMouseMove);
}

// Initial setup
attachInputListeners();

function updateMovement(deltaSeconds) {
    // W follows the camera forward vector; S reverses it. A/D use camera right.
    movement.forward = Number(keys.has("KeyW")) - Number(keys.has("KeyS"));
    movement.strafe = Number(keys.has("KeyD")) - Number(keys.has("KeyA"));

    const stats = WEAPON_STATS[weapon] || WEAPON_STATS["ASSAULT_RIFLE"];
    if (isFiring && stats.fullAuto) {
        attemptFire();
    }

    // Camera Recoil & Shake Updates
    targetRecoilPitch += (0 - targetRecoilPitch) * 8 * deltaSeconds;
    targetRecoilYaw += (0 - targetRecoilYaw) * 8 * deltaSeconds;
    currentRecoilPitch += (targetRecoilPitch - currentRecoilPitch) * 15 * deltaSeconds;
    currentRecoilYaw += (targetRecoilYaw - currentRecoilYaw) * 15 * deltaSeconds;

    if (shakeTime > 0) {
        shakeTime -= deltaSeconds;
        shakeIntensity += (0 - shakeIntensity) * 5 * deltaSeconds;
    } else {
        shakeIntensity = 0;
    }

    let shakePitch = 0, shakeYaw = 0, shakeRoll = 0;
    if (shakeIntensity > 0) {
        shakePitch = (Math.random() - 0.5) * shakeIntensity;
        shakeYaw = (Math.random() - 0.5) * shakeIntensity;
        shakeRoll = (Math.random() - 0.5) * shakeIntensity * 0.5;
    }

    const finalPitch = Math.max(-1.45, Math.min(1.45, pitch + currentRecoilPitch)) + shakePitch;
    const finalYaw = yaw + currentRecoilYaw + shakeYaw;
    renderer.camera.rotation.set(finalPitch, finalYaw, shakeRoll);

    // Spawn dust particles at feet when moving
    if (onGround && (movement.forward !== 0 || movement.strafe !== 0)) {
        if (Math.random() < 8 * deltaSeconds) {
            const feetPos = { 
                x: renderer.camera.position.x, 
                y: renderer.camera.position.y - currentEyeHeight, 
                z: renderer.camera.position.z 
            };
            renderer.spawnParticles(feetPos, {x: 0, y: 1, z: 0}, 'dust');
        }
    }

    if (keys.has("Space") && onGround) {
        if (stance === "STANDING") {
            sounds.unlock();
            sounds.jump();
            verticalVelocity = 6.5;
            onGround = false;
        } else {
            // Jumping from crouch/prone just stands up
            stance = "STANDING";
        }
    }
    
    if (!onGround) {
        stance = "JUMPING";
    } else if (stance === "JUMPING") {
        stance = "STANDING";
    }
    
    const targetEyeHeight = stance === "PRONE" ? 0.65 : stance === "CROUCHING" ? 1.15 : 1.7;
    currentEyeHeight += (targetEyeHeight - currentEyeHeight) * 12 * deltaSeconds;

    let feetY = renderer.camera.position.y - currentEyeHeight;

    if (!onGround) {
        verticalVelocity -= 18 * deltaSeconds;
        feetY += verticalVelocity * deltaSeconds;
        if (feetY <= 0) {
            feetY = 0;
            verticalVelocity = 0;
            onGround = true;
        }
    } else {
        feetY = 0;
        verticalVelocity = 0;
    }

    renderer.camera.position.y = feetY + currentEyeHeight;

    const baseSpeed = 4.8; // Tactical, controlled pace
    const speedMultiplier = stance === "PRONE" ? 0.25 : stance === "CROUCHING" ? 0.50 : 1.0;
    const targetSpeed = baseSpeed * speedMultiplier;
    
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

    // Smooth, weighty acceleration and deceleration (damping)
    const accelRate = onGround ? 6.0 : 1.5;
    const decelRate = onGround ? 7.0 : 0.5;
    const acceleration = (movement.forward !== 0 || movement.strafe !== 0) ? accelRate : decelRate;
    
    playerVelocityX += (targetVelX - playerVelocityX) * acceleration * deltaSeconds;
    playerVelocityZ += (targetVelZ - playerVelocityZ) * acceleration * deltaSeconds;

    const nextX = renderer.camera.position.x + playerVelocityX * deltaSeconds;
    const nextZ = renderer.camera.position.z + playerVelocityZ * deltaSeconds;

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
            y: feetY,
            z: renderer.camera.position.z
        },
        { x: 0, y: yaw, z: 0 },
        stance
    );
}

function publishLocalState() {
    const feetY = renderer.camera.position.y - currentEyeHeight;
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
    localSpawnSynced = false;
    stance = "STANDING";
    attachInputListeners();
    const lobbyScreen = document.querySelector("#lobby-screen");
    const lobbyWaitingScreen = document.querySelector("#lobby-waiting-screen");
    const gameShell = document.querySelector("#game-shell");
    const matchOverElement = document.querySelector("#match-over");
    
    if (lobbyScreen) lobbyScreen.hidden = true;
    if (lobbyWaitingScreen) lobbyWaitingScreen.style.display = "flex";
    if (gameShell) gameShell.hidden = true;
    if (matchOverElement) matchOverElement.hidden = true;

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
                        if (!localSpawnSynced && local.position) {
                            localSpawnSynced = true;
                            renderer.camera.position.set(local.position.x, local.position.y + 1.7, local.position.z);
                            renderer.camera.rotation.set(0, 0, 0);
                            pitch = 0; yaw = 0;
                        }
                    }
                    updateLiveScoreboard(state.players);
                }
            } catch (error) {
                console.error("Failed to parse remote players:", error, state.players);
            }
            if (state.roomState !== "ACTIVE") {
                matchTimerElement.textContent = state.roomState === "FINISHED" ? "Match over" : "Waiting for players";
                if (state.roomState === "FINISHED") {
                    sounds.playLobbyMusic();
                }
            } else if (state.remainingSeconds) {
                sounds.stopLobbyMusic();
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
                    renderer.remoteFire(remote);
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
    
    if (value < currentHealthVal) {
        shakeTime = 0.3;
        shakeIntensity = Math.min(shakeIntensity + 0.05, 0.1);
        
        // Damage vignette flash
        const vignette = document.getElementById("damage-vignette");
        if (vignette) {
            vignette.style.opacity = '1';
            setTimeout(() => {
                vignette.style.opacity = '0';
            }, 100);
        }
    }
    currentHealthVal = value;

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
    sounds.playLobbyMusic();
}

async function submitLobby(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const requestedRoom = String(form.get("roomCode") || "").trim().toUpperCase();
    if (!authToken || !accountNickname) {
        openNicknameSettings();
        showLobbyMessage("Save a unique nickname before entering the battleground.");
        return;
    }
    displayName = accountNickname;
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

    try {
        const jwtToken = authToken;

        const gameModeSelect = document.getElementById("game-mode-select").value;
        const modeParts = gameModeSelect.split("_");
        const gameModeStr = modeParts[0] === "TEAM" ? "TEAM_" + modeParts[1] : modeParts[0] + "_" + modeParts[1];
        const playerLimitNum = Number(modeParts[1]);

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
                    roomName: `${displayName}'s Arena`,
                    displayName,
                    map: String(form.get("map")),
                    playerLimit: playerLimitNum,
                    gameMode: gameModeStr
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
        btnText.textContent = "Enter Battleground";
        btnSpinner.hidden = true;
    }
}

document.querySelector("#lobby-form").addEventListener("submit", submitLobby);

document.addEventListener("click", () => {
    // A generic global click listener to unlock audio and trigger autoplay
    sounds.unlock();
}, { once: true });

if (displayName) {
    completeLogin(displayName);
    if (authToken) refreshSavedNickname(displayName);
}

if (roomCode && displayName) {
    startGame().catch((error) => {
        console.error(error);
        statusElement.textContent = "Unable to initialize the arena";
    });
}
