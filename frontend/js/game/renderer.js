import * as THREE from "https://cdn.jsdelivr.net/npm/three@0.168.0/build/three.module.js";
import { GLTFLoader } from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/loaders/GLTFLoader.js";
import { MODEL_BASE_URL } from "../config.js";

const WEAPON_MODEL_URLS = {
    PISTOL: `${MODEL_BASE_URL}/Desert%20Eagle.glb`,
    ASSAULT_RIFLE: `${MODEL_BASE_URL}/AKM.glb`,
    SMG: `${MODEL_BASE_URL}/UMP.glb`,
    SHOTGUN: `${MODEL_BASE_URL}/S686.glb`,
    BOLT_ACTION_RIFLE: `${MODEL_BASE_URL}/AWM.glb`
};

export class ArenaStrikeRenderer {
    constructor(canvas) {
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x081018);
        this.scene.fog = new THREE.Fog(0x081018, 35, 150);
        this.camera = new THREE.PerspectiveCamera(70, 1, 0.1, 250);
        this.camera.position.set(0, 1.7, 8);
        this.camera.rotation.order = "YXZ";
        this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this.loader = new GLTFLoader();
        this.remotePlayers = new Map();
        this.localPlayer = null;
        this.characterTemplate = null;
        this.gunTemplates = new Map();
        this.firstPersonWeapon = null;
        this.muzzleFlash = null;
        this.weaponRecoil = 0;
        this.aiming = false;
        this.raycastTargets = [];
        this.colliders = [];
        this.addLighting();
        this.resize();
        window.addEventListener("resize", () => this.resize());
    }

    async loadMap(map = "MAP_1") {
        if (map === "MAP_2") {
            this.addIndustrialOutpostMap();
        } else {
            this.addUrbanCourtyardMap();
        }
        return true;
    }

    addUrbanCourtyardMap() {
        const ground = new THREE.Mesh(
            new THREE.PlaneGeometry(120, 120),
            new THREE.MeshStandardMaterial({ color: 0x30363a, roughness: 0.9 })
        );
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;
        this.scene.add(ground);

        const roadMaterial = new THREE.MeshStandardMaterial({ color: 0x171b1e, roughness: 0.95 });
        this.addMapBox(0, 0.015, 0, 18, 0.03, 120, roadMaterial, false);
        this.addMapBox(0, 0.02, 0, 120, 0.03, 16, roadMaterial, false);

        const buildingMaterial = new THREE.MeshStandardMaterial({ color: 0x687178, roughness: 0.82 });
        const roofMaterial = new THREE.MeshStandardMaterial({ color: 0x343c42, roughness: 0.9 });
        [
            [-37, 5, -34, 24, 10, 20], [37, 5, -34, 24, 10, 20],
            [-37, 5, 34, 24, 10, 20], [37, 5, 34, 24, 10, 20],
            [-8, 3, -42, 12, 6, 8], [8, 3, 42, 12, 6, 8]
        ].forEach(([x, y, z, width, height, depth]) => {
            this.addMapBox(x, y, z, width, height, depth, buildingMaterial);
            this.addMapBox(x, height + 0.15, z, width + 0.5, 0.3, depth + 0.5, roofMaterial, false);
        });

        const coverMaterial = new THREE.MeshStandardMaterial({ color: 0x9b7654, roughness: 0.95 });
        [
            [-24, 1, -12, 8, 2, 2], [24, 1, -12, 8, 2, 2],
            [-24, 1, 12, 8, 2, 2], [24, 1, 12, 8, 2, 2],
            [-8, 0.8, 26, 3, 1.6, 3], [8, 0.8, -26, 3, 1.6, 3]
        ].forEach(([x, y, z, width, height, depth]) =>
            this.addMapBox(x, y, z, width, height, depth, coverMaterial)
        );

        const courtyardMaterial = new THREE.MeshStandardMaterial({ color: 0x59666b, roughness: 0.88 });
        this.addMapBox(0, 0.08, 0, 28, 0.16, 28, courtyardMaterial, false);
        const fountainMaterial = new THREE.MeshStandardMaterial({ color: 0x4c9db0, roughness: 0.3 });
        const fountain = new THREE.Mesh(
            new THREE.CylinderGeometry(4, 4, 0.3, 32),
            fountainMaterial
        );
        fountain.position.set(0, 0.25, 0);
        fountain.receiveShadow = true;
        this.scene.add(fountain);
        this.addMapBox(0, 1.2, 0, 1.2, 2.4, 1.2, coverMaterial);
    }

    addIndustrialOutpostMap() {
        const ground = new THREE.Mesh(
            new THREE.PlaneGeometry(120, 120),
            new THREE.MeshStandardMaterial({ color: 0x625b4b, roughness: 0.95 })
        );
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;
        this.scene.add(ground);

        const roadMaterial = new THREE.MeshStandardMaterial({ color: 0x252422, roughness: 0.92 });
        this.addMapBox(0, 0.02, 0, 14, 0.04, 120, roadMaterial, false);
        this.addMapBox(0, 0.025, 0, 120, 0.04, 14, roadMaterial, false);

        const containerMaterial = new THREE.MeshStandardMaterial({ color: 0x8b3f32, roughness: 0.8 });
        const darkContainerMaterial = new THREE.MeshStandardMaterial({ color: 0x315866, roughness: 0.8 });
        [
            [-30, 2, -28, 18, 4, 4], [-30, 2, -20, 18, 4, 4],
            [30, 2, 28, 18, 4, 4], [30, 2, 20, 18, 4, 4],
            [-22, 2, 28, 4, 4, 18], [22, 2, -28, 4, 4, 18]
        ].forEach(([x, y, z, width, height, depth], index) =>
            this.addMapBox(x, y, z, width, height, depth,
                index % 2 ? darkContainerMaterial : containerMaterial)
        );

        const warehouseMaterial = new THREE.MeshStandardMaterial({ color: 0x77736a, roughness: 0.9 });
        this.addMapBox(-38, 5, 0, 14, 10, 24, warehouseMaterial);
        this.addMapBox(38, 5, 0, 14, 10, 24, warehouseMaterial);

        const coverMaterial = new THREE.MeshStandardMaterial({ color: 0x786044, roughness: 0.95 });
        [
            [-10, 1, -10, 6, 2, 2], [10, 1, -10, 6, 2, 2],
            [-10, 1, 10, 6, 2, 2], [10, 1, 10, 6, 2, 2],
            [0, 1.2, -28, 2, 2.4, 2], [0, 1.2, 28, 2, 2.4, 2]
        ].forEach(([x, y, z, width, height, depth]) =>
            this.addMapBox(x, y, z, width, height, depth, coverMaterial)
        );
    }

    addMapBox(x, y, z, width, height, depth, material, collidable = true) {
        const mesh = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), material);
        mesh.position.set(x, y, z);
        mesh.castShadow = collidable;
        mesh.receiveShadow = true;
        this.scene.add(mesh);
        if (collidable) {
            this.colliders.push(new THREE.Box3().setFromObject(mesh));
        }
        return mesh;
    }

    async loadPlayerAssets(characterUrl = `${MODEL_BASE_URL}/Character.glb`) {
        const weaponEntries = Object.entries(WEAPON_MODEL_URLS);
        const [character, ...weapons] = await Promise.allSettled([
            this.loader.loadAsync(characterUrl),
            ...weaponEntries.map(([, url]) => this.loader.loadAsync(url))
        ]);
        this.characterTemplate = character.status === "fulfilled" ? character.value.scene : null;
        if (this.characterTemplate) {
            this.normalizeModel(this.characterTemplate, 1.8);
        }
        weapons.forEach((result, index) => {
            if (result.status === "fulfilled") {
                const gun = result.value.scene;
                this.normalizeModel(gun, 1.25);
                this.gunTemplates.set(weaponEntries[index][0], gun);
            }
        });
    }

    normalizeModel(model, targetSize) {
        model.updateMatrixWorld(true);
        const bounds = new THREE.Box3().setFromObject(model);
        const size = bounds.getSize(new THREE.Vector3());
        const largestDimension = Math.max(size.x, size.y, size.z);
        if (largestDimension > 0) {
            model.scale.multiplyScalar(targetSize / largestDimension);
        }
    }

    addLighting() {
        this.scene.add(new THREE.HemisphereLight(0xb8d5e8, 0x18212a, 1.8));
        const sun = new THREE.DirectionalLight(0xffffff, 2.4);
        sun.position.set(15, 25, 10);
        sun.castShadow = true;
        sun.shadow.mapSize.set(2048, 2048);
        this.scene.add(sun);
    }

    addPlaceholderMap() {
        const floor = new THREE.Mesh(
            new THREE.PlaneGeometry(120, 120),
            new THREE.MeshStandardMaterial({ color: 0x263238, roughness: 0.92 })
        );
        floor.rotation.x = -Math.PI / 2;
        floor.receiveShadow = true;
        this.scene.add(floor);
        const material = new THREE.MeshStandardMaterial({ color: 0x66747c, roughness: 0.8 });
        [[0, 2, -12, 18, 4, 2], [-12, 1.5, -2, 2, 3, 16],
            [12, 1.5, 4, 2, 3, 16], [-5, 1, 8, 8, 2, 2],
            [7, 1, -4, 6, 2, 2]].forEach(([x, y, z, width, height, depth]) => {
            const obstacle = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), material);
            obstacle.position.set(x, y, z);
            obstacle.castShadow = true;
            obstacle.receiveShadow = true;
            this.scene.add(obstacle);
        });
    }

    createPlayerModel(color, weapon = "ASSAULT_RIFLE") {
        const root = new THREE.Group();
        const body = this.characterTemplate
            ? this.characterTemplate.clone(true)
            : new THREE.Mesh(new THREE.CapsuleGeometry(0.35, 1.1, 4, 8),
                new THREE.MeshStandardMaterial({ color }));
        body.traverse((object) => {
            if (object.isMesh) {
                object.castShadow = true;
                object.receiveShadow = true;
                object.userData.hitLocation = object.name.toLowerCase().includes("head")
                    ? "HEAD"
                    : object.name.toLowerCase().includes("arm") || object.name.toLowerCase().includes("leg")
                        ? "LIMB" : "TORSO";
            }
        });
        root.add(body);
        root.add(this.createWeaponModel(weapon, false));
        root.userData.walkPhase = Math.random() * Math.PI * 2;
        return root;
    }

    createWeaponModel(weapon, firstPerson) {
        const gunTemplate = this.gunTemplates.get(weapon)
            || this.gunTemplates.get("ASSAULT_RIFLE");
        const gun = gunTemplate
            ? gunTemplate.clone(true)
            : new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.12, 0.8),
                new THREE.MeshStandardMaterial({ color: 0x20252a }));
        // The assets point along +X. +90 degrees around Y points the muzzle
        // toward the camera's -Z forward axis.
        gun.rotation.set(0, Math.PI / 2, 0);
        gun.position.set(firstPerson ? 0.22 : 0.35, firstPerson ? -0.38 : 1.05,
            firstPerson ? -0.62 : -0.5);
        gun.scale.setScalar(firstPerson ? 0.3 : 1);
        return gun;
    }

    spawnLocalPlayer(weapon = "ASSAULT_RIFLE") {
        this.localPlayer = this.createPlayerModel(0x4ea5d9, weapon);
        this.localPlayer.visible = false;
        this.scene.add(this.localPlayer);
        this.firstPersonWeapon = this.createWeaponModel(weapon, true);
        this.camera.add(this.firstPersonWeapon);
        this.muzzleFlash = new THREE.Mesh(
            new THREE.SphereGeometry(0.09, 8, 8),
            new THREE.MeshBasicMaterial({ color: 0xffc34d })
        );
        this.muzzleFlash.position.set(0, 0, -0.72);
        this.muzzleFlash.visible = false;
        this.camera.add(this.muzzleFlash);
        this.scene.add(this.camera);
    }

    setAiming(aiming) {
        this.aiming = aiming;
    }

    fireWeapon() {
        if (!this.firstPersonWeapon) {
            return;
        }
        this.weaponRecoil = 0.08;
        if (this.muzzleFlash) {
            this.muzzleFlash.visible = true;
            window.setTimeout(() => {
                if (this.muzzleFlash) {
                    this.muzzleFlash.visible = false;
                }
            }, 55);
        }
    }

    updateLocalPlayer(position, rotation, stance) {
        if (this.localPlayer) {
            this.localPlayer.position.set(position.x, position.y, position.z);
            this.localPlayer.rotation.y = rotation.y;
            this.applyStance(this.localPlayer, stance);
        }
    }

    canMoveTo(x, z, feetY, radius = 0.45) {
        return !this.colliders.some((bounds) => {
            const overlapsHeight = feetY < bounds.max.y && feetY + 1.8 > bounds.min.y;
            const overlapsX = x > bounds.min.x - radius && x < bounds.max.x + radius;
            const overlapsZ = z > bounds.min.z - radius && z < bounds.max.z + radius;
            return overlapsHeight && overlapsX && overlapsZ;
        });
    }

    updateRemotePlayers(players, localPlayerId) {
        const activeIds = new Set(players.map((player) => player.playerId));
        this.remotePlayers.forEach((remote, playerId) => {
            if (!activeIds.has(playerId) || playerId === localPlayerId) {
                this.scene.remove(remote.mesh);
                this.remotePlayers.delete(playerId);
            }
        });
        players.forEach((player) => {
            if (player.playerId === localPlayerId) {
                return;
            }
            let remote = this.remotePlayers.get(player.playerId);
            if (!remote) {
                remote = {
                    mesh: this.createPlayerModel(0xe05d44, player.currentWeapon),
                    target: player,
                    weapon: player.currentWeapon
                };
                this.scene.add(remote.mesh);
                this.remotePlayers.set(player.playerId, remote);
            }
            if (remote.weapon !== player.currentWeapon) {
                this.scene.remove(remote.mesh);
                remote.mesh = this.createPlayerModel(0xe05d44, player.currentWeapon);
                this.scene.add(remote.mesh);
                remote.weapon = player.currentWeapon;
            }
            remote.target = player;
        });
        this.raycastTargets = [...this.remotePlayers.entries()].map(([playerId, remote]) => ({
            playerId, mesh: remote.mesh
        }));
    }

    raycastOpponent() {
        const raycaster = new THREE.Raycaster();
        raycaster.setFromCamera(new THREE.Vector2(0, 0), this.camera);
        const intersections = raycaster.intersectObjects(
            this.raycastTargets.map((target) => target.mesh), true);
        if (!intersections.length) {
            return null;
        }
        let object = intersections[0].object;
        while (object.parent && !this.raycastTargets.some((target) => target.mesh === object)) {
            object = object.parent;
        }
        const target = this.raycastTargets.find((entry) => entry.mesh === object);
        return target ? {
            targetId: target.playerId,
            hitLocation: intersections[0].object.userData.hitLocation || "TORSO"
        } : null;
    }

    interpolateRemotePlayers(deltaSeconds) {
        const blend = 1 - Math.exp(-12 * deltaSeconds);
        this.remotePlayers.forEach(({ mesh, target }) => {
            mesh.position.lerp(new THREE.Vector3(target.position.x, target.position.y, target.position.z), blend);
            mesh.rotation.y = THREE.MathUtils.lerp(mesh.rotation.y, target.rotation.y, blend);
            this.applyStance(mesh, target.stance);
        });
    }

    applyStance(model, stance) {
        const y = stance === "PRONE" ? 0.45 : stance === "CROUCHING" ? 0.7 : 1;
        model.scale.set(1, y, stance === "PRONE" ? 1.25 : 1);
    }

    render() {
        const targetFov = this.aiming ? 45 : 70;
        this.camera.fov = THREE.MathUtils.lerp(this.camera.fov, targetFov, 0.2);
        this.camera.updateProjectionMatrix();
        if (this.firstPersonWeapon) {
            const targetX = this.aiming ? 0 : 0.22;
            const targetY = this.aiming ? -0.25 : -0.38;
            this.firstPersonWeapon.position.x = THREE.MathUtils.lerp(
                this.firstPersonWeapon.position.x, targetX, 0.2
            );
            this.firstPersonWeapon.position.y = THREE.MathUtils.lerp(
                this.firstPersonWeapon.position.y, targetY, 0.2
            );
            this.firstPersonWeapon.position.z = THREE.MathUtils.lerp(
                this.firstPersonWeapon.position.z, -0.62 + this.weaponRecoil, 0.3
            );
            this.weaponRecoil *= 0.72;
        }
        this.renderer.render(this.scene, this.camera);
    }

    resize() {
        const width = this.renderer.domElement.clientWidth;
        const height = this.renderer.domElement.clientHeight;
        if (width && height) {
            this.camera.aspect = width / height;
            this.camera.updateProjectionMatrix();
            this.renderer.setSize(width, height, false);
        }
    }
}
