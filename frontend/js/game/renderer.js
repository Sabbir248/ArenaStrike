import * as THREE from "https://cdn.jsdelivr.net/npm/three@0.168.0/build/three.module.js";
import { GLTFLoader } from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/loaders/GLTFLoader.js";
import * as SkeletonUtils from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/utils/SkeletonUtils.js";
import { EffectComposer } from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/postprocessing/EffectComposer.js";
import { RenderPass } from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/postprocessing/RenderPass.js";
import { UnrealBloomPass } from "https://cdn.jsdelivr.net/npm/three@0.168.0/examples/jsm/postprocessing/UnrealBloomPass.js";
import { MODEL_BASE_URL } from "../config.js";
import { createTacticalSoldierModel } from "./soldier.js";
import { createDesertEagle, createAKM, createUMP, createS686, createAWM } from "./weapons.js";

const WEAPON_CONFIGS = {
    ASSAULT_RIFLE: {
        position: new THREE.Vector3(0.22, -0.20, -0.38),
        rotation: new THREE.Euler(0, 0.05, 0),
        scale: new THREE.Vector3(1, 1, 1),
        adsPosition: new THREE.Vector3(0, -0.15, -0.30)
    },
    SHOTGUN: {
        position: new THREE.Vector3(0.24, -0.22, -0.42),
        rotation: new THREE.Euler(0, 0.05, 0),
        scale: new THREE.Vector3(1, 1, 1),
        adsPosition: new THREE.Vector3(0, -0.15, -0.30)
    },
    PISTOL: {
        position: new THREE.Vector3(0.20, -0.20, -0.38),
        rotation: new THREE.Euler(0, 0.05, 0),
        scale: new THREE.Vector3(1, 1, 1),
        adsPosition: new THREE.Vector3(0, -0.12, -0.25)
    },
    SMG: {
        position: new THREE.Vector3(0.22, -0.20, -0.38),
        rotation: new THREE.Euler(0, 0.05, 0),
        scale: new THREE.Vector3(1, 1, 1),
        adsPosition: new THREE.Vector3(0, -0.15, -0.30)
    },
    BOLT_ACTION_RIFLE: {
        position: new THREE.Vector3(0.25, -0.22, -0.40),
        rotation: new THREE.Euler(0, 0.05, 0),
        scale: new THREE.Vector3(1, 1, 1),
        adsPosition: new THREE.Vector3(0, -0.15, -0.30)
    }
};

export class ArenaStrikeRenderer {
    constructor(canvas) {
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x101520);
        this.scene.fog = new THREE.Fog(0x101520, 20, 150);
        this.camera = new THREE.PerspectiveCamera(70, 1, 0.1, 250);
        this.camera.position.set(0, 1.7, 8);
        this.camera.rotation.order = "YXZ";
        this.viewmodelScene = new THREE.Scene();
        this.viewmodelCamera = new THREE.PerspectiveCamera(70, 1, 0.01, 100);
        this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
        this.pixelRatio = 0;
        this.renderer.autoClear = false;
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        
        this.composer = new EffectComposer(this.renderer);
        const renderPass = new RenderPass(this.scene, this.camera);
        this.composer.addPass(renderPass);
        
        const viewmodelPass = new RenderPass(this.viewmodelScene, this.viewmodelCamera);
        viewmodelPass.clear = false;
        const originalRender = viewmodelPass.render;
        viewmodelPass.render = function(renderer, writeBuffer, readBuffer, deltaTime, maskActive) {
            renderer.clearDepth();
            originalRender.call(this, renderer, writeBuffer, readBuffer, deltaTime, maskActive);
        };
        this.composer.addPass(viewmodelPass);
        
        // Tactical Bloom: Threshold 0.85 ensures only very bright objects (e.g. muzzle flash) glow
        const bloomPass = new UnrealBloomPass(new THREE.Vector2(window.innerWidth, window.innerHeight), 1.0, 0.5, 0.85);
        this.composer.addPass(bloomPass);

        this.loader = new GLTFLoader();
        this.remotePlayers = new Map();
        this.localPlayer = null;
        this.characterTemplate = null;
        this.gunTemplates = new Map();
        this.firstPersonWeapon = null;
        this.muzzleLight = null;
        this.muzzleFlash = null;
        this.hitMarkerTimeout = null;
        this.weaponRecoil = 0;
        
        // Tracer Pool
        this.tracers = [];
        const tracerGeo = new THREE.CylinderGeometry(0.015, 0.015, 1, 4);
        tracerGeo.rotateX(Math.PI / 2);
        const tracerMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 1, blending: THREE.AdditiveBlending, depthWrite: false });
        for (let i = 0; i < 20; i++) {
            const t = new THREE.Mesh(tracerGeo, tracerMat.clone());
            t.visible = false;
            this.scene.add(t);
            this.tracers.push({ mesh: t, active: false, life: 0, startPos: new THREE.Vector3(), endPos: new THREE.Vector3() });
        }
        
        // Particle Pool
        this.particlePool = [];
        const particleGeo = new THREE.BoxGeometry(0.04, 0.04, 0.04);
        for (let i = 0; i < 150; i++) {
            const pMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 1 });
            const p = new THREE.Mesh(particleGeo, pMat);
            p.visible = false;
            this.scene.add(p);
            this.particlePool.push({
                mesh: p, active: false, life: 0, maxLife: 1, 
                velocity: new THREE.Vector3(), 
                gravity: 9.8, type: 'spark'
            });
        }

        this.aiming = false;
        this.lastCameraPos = new THREE.Vector3();
        this.bobPhase = 0;
        this.raycastTargets = [];
        this.colliders = [];
        this.addLighting();
        this.resize();
        window.addEventListener("resize", () => this.resize());
    }

    async loadMap(map = "MAP_WAREHOUSE") {
        if (map === "MAP_BUNKER") {
            this.addIndustrialOutpostMap();
        } else {
            this.addUrbanCourtyardMap();
        }
        return true;
    }

    addUrbanCourtyardMap() {
        const ground = new THREE.Mesh(
            new THREE.PlaneGeometry(120, 120),
            this.getMaterial("concrete", 0x30363a)
        );
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;
        this.scene.add(ground);

        const roadMaterial = this.getMaterial("concrete", 0x171b1e);
        this.addMapBox(0, 0.015, 0, 18, 0.03, 120, roadMaterial, false);
        this.addMapBox(0, 0.02, 0, 120, 0.03, 16, roadMaterial, false);

        const buildingMaterial = this.getMaterial("concrete", 0x687178);
        const roofMaterial = this.getMaterial("metal", 0x343c42);
        [
            [-37, 5, -34, 24, 10, 20], [37, 5, -34, 24, 10, 20],
            [-37, 5, 34, 24, 10, 20], [37, 5, 34, 24, 10, 20],
            [-8, 3, -42, 12, 6, 8], [8, 3, 42, 12, 6, 8]
        ].forEach(([x, y, z, width, height, depth]) => {
            this.addMapBox(x, y, z, width, height, depth, buildingMaterial);
            this.addMapBox(x, height + 0.15, z, width + 0.5, 0.3, depth + 0.5, roofMaterial, false);
        });

        const coverMaterial = this.getMaterial("wood", 0x9b7654);
        [
            [-24, 1, -12, 8, 2, 2], [24, 1, -12, 8, 2, 2],
            [-24, 1, 12, 8, 2, 2], [24, 1, 12, 8, 2, 2],
            [-8, 0.8, 26, 3, 1.6, 3], [8, 0.8, -26, 3, 1.6, 3]
        ].forEach(([x, y, z, width, height, depth]) =>
            this.addMapBox(x, y, z, width, height, depth, coverMaterial)
        );

        const courtyardMaterial = this.getMaterial("concrete", 0x59666b);
        this.addMapBox(0, 0.08, 0, 28, 0.16, 28, courtyardMaterial, false);
        const fountainMaterial = this.getMaterial("metal", 0x4c9db0);
        const fountain = new THREE.Mesh(
            new THREE.CylinderGeometry(4, 4, 0.3, 32),
            fountainMaterial
        );
        fountain.position.set(0, 0.25, 0);
        fountain.receiveShadow = true;
        fountain.castShadow = true;
        this.scene.add(fountain);
        this.addMapBox(0, 1.2, 0, 1.2, 2.4, 1.2, coverMaterial);
        
        // Add environmental props
        const pipeMaterial = this.getMaterial("metal", 0x222222);
        this.addMapBox(12, 0.5, 0, 0.5, 1, 18, pipeMaterial);
        this.addMapBox(-12, 0.5, 0, 0.5, 1, 18, pipeMaterial);
    }

    addIndustrialOutpostMap() {
        const ground = new THREE.Mesh(
            new THREE.PlaneGeometry(120, 120),
            this.getMaterial("concrete", 0x625b4b)
        );
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;
        this.scene.add(ground);

        const roadMaterial = this.getMaterial("concrete", 0x252422);
        this.addMapBox(0, 0.02, 0, 14, 0.04, 120, roadMaterial, false);
        this.addMapBox(0, 0.025, 0, 120, 0.04, 14, roadMaterial, false);

        const containerMaterial = this.getMaterial("metal", 0x8b3f32);
        const darkContainerMaterial = this.getMaterial("metal", 0x315866);
        [
            [-30, 2, -28, 18, 4, 4], [-30, 2, -20, 18, 4, 4],
            [30, 2, 28, 18, 4, 4], [30, 2, 20, 18, 4, 4],
            [-22, 2, 28, 4, 4, 18], [22, 2, -28, 4, 4, 18]
        ].forEach(([x, y, z, width, height, depth], index) =>
            this.addMapBox(x, y, z, width, height, depth,
                index % 2 ? darkContainerMaterial : containerMaterial)
        );

        const warehouseMaterial = this.getMaterial("concrete", 0x77736a);
        this.addMapBox(-38, 5, 0, 14, 10, 24, warehouseMaterial);
        this.addMapBox(38, 5, 0, 14, 10, 24, warehouseMaterial);

        const coverMaterial = this.getMaterial("wood", 0x786044);
        [
            [-10, 1, -10, 6, 2, 2], [10, 1, -10, 6, 2, 2],
            [-10, 1, 10, 6, 2, 2], [10, 1, 10, 6, 2, 2],
            [0, 1.2, -28, 2, 2.4, 2], [0, 1.2, 28, 2, 2.4, 2]
        ].forEach(([x, y, z, width, height, depth]) =>
            this.addMapBox(x, y, z, width, height, depth, coverMaterial)
        );
        
        // Add environmental props
        const barrierMaterial = this.getMaterial("concrete", 0xaaaaaa);
        this.addMapBox(0, 0.5, 18, 6, 1, 0.5, barrierMaterial);
        this.addMapBox(0, 0.5, -18, 6, 1, 0.5, barrierMaterial);
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

    async loadPlayerAssets() {
        return Promise.resolve();
    }

    normalizeModel(model, targetSize, alignToGround = false) {
        model.updateMatrixWorld(true);
        const bounds = new THREE.Box3().setFromObject(model);
        const size = bounds.getSize(new THREE.Vector3());
        const largestDimension = Math.max(size.x, size.y, size.z);
        if (largestDimension > 0) {
            model.scale.multiplyScalar(targetSize / largestDimension);
        }
        if (alignToGround) {
            model.updateMatrixWorld(true);
            const newBounds = new THREE.Box3().setFromObject(model);
            model.position.y -= newBounds.min.y;
        }
    }

    addLighting() {
        // Deep ambient base for rich, pitch-black shadows
        this.scene.add(new THREE.AmbientLight(0x0a0f15));
        
        // Hemisphere light adds realistic sky-bounce and ground-bounce without washing out shadows
        const hemiLight = new THREE.HemisphereLight(0x1a2638, 0x05080c, 1.2);
        this.scene.add(hemiLight);
        
        // Stronger, cooler moon light for sharp, dramatic shadows
        const moon = new THREE.DirectionalLight(0x7799bb, 2.0);
        moon.position.set(50, 80, 20);
        moon.castShadow = true;
        moon.shadow.mapSize.set(2048, 2048);
        moon.shadow.camera.left = -60;
        moon.shadow.camera.right = 60;
        moon.shadow.camera.top = 60;
        moon.shadow.camera.bottom = -60;
        moon.shadow.camera.near = 10;
        moon.shadow.camera.far = 200;
        moon.shadow.bias = -0.0005;
        this.scene.add(moon);
        
        // Viewmodel specific lighting to make the first-person weapon look crisp
        this.viewmodelScene.add(new THREE.AmbientLight(0x1a2638));
        const viewmodelMoon = new THREE.DirectionalLight(0x7799bb, 2.0);
        viewmodelMoon.position.set(50, 80, 20);
        this.viewmodelScene.add(viewmodelMoon);
        
        const skyGeo = new THREE.SphereGeometry(200, 32, 32);
        const skyMat = new THREE.MeshBasicMaterial({
            map: this.createSkyTexture(),
            side: THREE.BackSide,
            fog: false
        });
        this.scene.add(new THREE.Mesh(skyGeo, skyMat));
    }

    createSkyTexture() {
        const canvas = document.createElement("canvas");
        canvas.width = 1024;
        canvas.height = 1024;
        const ctx = canvas.getContext("2d");
        const gradient = ctx.createLinearGradient(0, 0, 0, 1024);
        gradient.addColorStop(0, "#081018");
        gradient.addColorStop(0.5, "#101520");
        gradient.addColorStop(1, "#1a2430");
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, 1024, 1024);
        ctx.fillStyle = "#ffffff";
        for (let i = 0; i < 800; i++) {
            ctx.globalAlpha = Math.random();
            ctx.beginPath();
            ctx.arc(Math.random() * 1024, Math.random() * 1024, Math.random() * 1.2, 0, Math.PI * 2);
            ctx.fill();
        }
        return new THREE.CanvasTexture(canvas);
    }

    createNoiseMap(baseColorHex, variance, repeat) {
        const canvas = document.createElement("canvas");
        canvas.width = 256;
        canvas.height = 256;
        const ctx = canvas.getContext("2d");
        const imgData = ctx.createImageData(256, 256);
        
        const rBase = (baseColorHex >> 16) & 255;
        const gBase = (baseColorHex >> 8) & 255;
        const bBase = baseColorHex & 255;
        
        for (let i = 0; i < imgData.data.length; i += 4) {
            const v = (Math.random() - 0.5) * variance;
            imgData.data[i] = Math.max(0, Math.min(255, rBase + v));
            imgData.data[i + 1] = Math.max(0, Math.min(255, gBase + v));
            imgData.data[i + 2] = Math.max(0, Math.min(255, bBase + v));
            imgData.data[i + 3] = 255;
        }
        ctx.putImageData(imgData, 0, 0);
        const tex = new THREE.CanvasTexture(canvas);
        tex.wrapS = THREE.RepeatWrapping;
        tex.wrapT = THREE.RepeatWrapping;
        tex.repeat.set(repeat, repeat);
        return tex;
    }

    getMaterial(type, baseColorHex) {
        const key = `${type}_${baseColorHex}`;
        if (!this.materials) this.materials = {};
        if (this.materials[key]) return this.materials[key];
        
        let mat;
        if (type === "concrete") {
            const map = this.createNoiseMap(baseColorHex, 30, 8);
            const rough = this.createNoiseMap(0xcccccc, 20, 8);
            mat = new THREE.MeshStandardMaterial({ map: map, roughnessMap: rough, roughness: 0.9, metalness: 0.1 });
        } else if (type === "metal") {
            const map = this.createNoiseMap(baseColorHex, 20, 4);
            const rough = this.createNoiseMap(0x888888, 30, 4);
            mat = new THREE.MeshStandardMaterial({ map: map, roughnessMap: rough, roughness: 0.4, metalness: 0.8 });
        } else if (type === "wood") {
            const canvas = document.createElement("canvas");
            canvas.width = 256; canvas.height = 256;
            const ctx = canvas.getContext("2d");
            ctx.fillStyle = `#${baseColorHex.toString(16).padStart(6, '0')}`;
            ctx.fillRect(0,0,256,256);
            ctx.fillStyle = "#000000";
            for(let i=0; i<40; i++) {
                ctx.globalAlpha = Math.random() * 0.2;
                ctx.fillRect(0, Math.random()*256, 256, Math.random()*4 + 1);
            }
            const map = new THREE.CanvasTexture(canvas);
            map.wrapS = THREE.RepeatWrapping; map.wrapT = THREE.RepeatWrapping;
            map.repeat.set(4, 4);
            mat = new THREE.MeshStandardMaterial({ map: map, roughness: 0.8, metalness: 0.05 });
        } else {
            mat = new THREE.MeshStandardMaterial({ color: baseColorHex, roughness: 0.8 });
        }
        this.materials[key] = mat;
        return mat;
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

    createPlayerModel(colorHex, weapon = "ASSAULT_RIFLE") {
        try {
            const root = createTacticalSoldierModel(colorHex);
            
            try {
                const weaponMesh = this.createWeaponModel(weapon, false);
                // Attach weapon to the RightHandSocket defined in soldier.js
                const rightHandSocket = root.getObjectByName("RightHandSocket");
                if (rightHandSocket) {
                    rightHandSocket.add(weaponMesh);
                } else {
                    weaponMesh.position.set(0.3, 0.9, -0.5);
                    root.add(weaponMesh);
                }
            } catch (wErr) {
                console.error("[THREE.js Error] Failed to create weapon model:", wErr);
            }
            
            // FIX: The model's pivot point might be off causing it to float mid-air.
            // Dynamically calculate the bounding box and wrap in a container
            // so the feet (min.y) are exactly aligned with the floor (y=0).
            const container = new THREE.Group();
            root.updateMatrixWorld(true);
            const boundingBox = new THREE.Box3().setFromObject(root);
            root.position.y = -boundingBox.min.y;
            
            container.add(root);
            // Transfer userData references so animations still work on the nested root
            container.userData = root.userData;
            
            return container;
        } catch (err) {
            console.error("[THREE.js Error] Failed to create player model:", err);
            return new THREE.Group();
        }
    }

    createWeaponModel(weapon, firstPerson) {
        let gun;
        switch (weapon) {
            case "PISTOL": gun = createDesertEagle(); break;
            case "SMG": gun = createUMP(); break;
            case "SHOTGUN": gun = createS686(); break;
            case "BOLT_ACTION_RIFLE": gun = createAWM(); break;
            case "ASSAULT_RIFLE":
            default: gun = createAKM(); break;
        }
        
        if (firstPerson) {
            const config = WEAPON_CONFIGS[weapon] || WEAPON_CONFIGS["ASSAULT_RIFLE"];
            gun.rotation.copy(config.rotation);
            gun.position.copy(config.position);
            gun.scale.copy(config.scale);
        } else {
            gun.rotation.set(Math.PI / 2, Math.PI, 0);
            gun.position.set(0, 0, 0);
            gun.scale.setScalar(1);
        }
        return gun;
    }

    spawnLocalPlayer(weapon = "ASSAULT_RIFLE") {
        // A room/game start callback can run more than once. Remove the old
        // viewmodel before creating its replacement so an idle gun cannot be
        // left behind underneath the newly animated one.
        const disposeModel = (model) => {
            if (!model) return;
            model.traverse((object) => {
                if (!object.isMesh) return;
                if (object.geometry) object.geometry.dispose();
                if (object.material) {
                    if (Array.isArray(object.material)) {
                        object.material.forEach((material) => this.disposeMaterial(material));
                    } else {
                        this.disposeMaterial(object.material);
                    }
                }
            });
            if (model.parent) model.parent.remove(model);
        };
        disposeModel(this.firstPersonWeapon);
        disposeModel(this.localPlayer);
        this.muzzleLight = null;

        this.currentWeaponType = weapon;
        this.localPlayer = this.createPlayerModel(0x4ea5d9, weapon);
        this.localPlayer.visible = false;
        this.scene.add(this.localPlayer);
        this.firstPersonWeapon = this.createWeaponModel(weapon, true);
        this.viewmodelCamera.add(this.firstPersonWeapon);
        
        let muzzleSocket = this.firstPersonWeapon.getObjectByName("muzzleSocket");
        if (!muzzleSocket) {
            muzzleSocket = new THREE.Group();
            muzzleSocket.name = "muzzleSocket";
            muzzleSocket.position.set(0, 0, -0.5);
            this.firstPersonWeapon.add(muzzleSocket);
        }

        this.muzzleLight = new THREE.PointLight(0xffaa33, 0, 4.0);
        muzzleSocket.add(this.muzzleLight);
        
        const flashMat = new THREE.SpriteMaterial({ color: 0xffcc77, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false });
        this.muzzleFlash = new THREE.Sprite(flashMat);
        this.muzzleFlash.scale.set(0.15, 0.15, 0.15);
        this.muzzleFlash.position.set(0, 0, -0.05); 
        this.muzzleFlash.visible = false;
        muzzleSocket.add(this.muzzleFlash);

        this.viewmodelScene.add(this.viewmodelCamera);
        this.scene.add(this.camera);
    }

    setAiming(aiming) {
        this.aiming = aiming;
        const crosshair = document.querySelector("#crosshair");
        const sniperOverlay = document.querySelector("#sniper-overlay");
        const redDotOverlay = document.querySelector("#red-dot-overlay");
        
        if (aiming) {
            if (crosshair) crosshair.hidden = true;
            if (this.currentWeaponType === "BOLT_ACTION_RIFLE") {
                if (sniperOverlay) sniperOverlay.hidden = false;
                if (redDotOverlay) redDotOverlay.hidden = true;
                if (this.firstPersonWeapon) this.firstPersonWeapon.visible = false;
            } else {
                if (sniperOverlay) sniperOverlay.hidden = true;
                if (redDotOverlay) redDotOverlay.hidden = false;
                if (this.firstPersonWeapon) this.firstPersonWeapon.visible = true;
            }
        } else {
            if (crosshair) crosshair.hidden = false;
            if (sniperOverlay) sniperOverlay.hidden = true;
            if (redDotOverlay) redDotOverlay.hidden = true;
            if (this.firstPersonWeapon) this.firstPersonWeapon.visible = true;
        }
    }

    fireWeapon(pitch, yaw) {
        if (!this.firstPersonWeapon) return;
        this.weaponRecoil = 0.08;
        
        if (this.muzzleLight) {
            this.muzzleLight.intensity = 2.0;
            window.setTimeout(() => { if (this.muzzleLight) this.muzzleLight.intensity = 0; }, 40);
        }
        if (this.muzzleFlash) {
            this.muzzleFlash.visible = true;
            this.muzzleFlash.material.rotation = Math.random() * Math.PI;
            this.muzzleFlash.material.opacity = 1.0;
            window.setTimeout(() => { if (this.muzzleFlash) this.muzzleFlash.visible = false; }, 40);
        }
        
        if (pitch !== undefined && yaw !== undefined) {
            const startPoint = new THREE.Vector3();
            const ms = this.firstPersonWeapon.getObjectByName("muzzleSocket");
            if (ms) {
                ms.getWorldPosition(startPoint);
                startPoint.add(this.camera.position);
            } else {
                startPoint.copy(this.camera.position);
            }
            
            const direction = new THREE.Vector3();
            this.camera.getWorldDirection(direction);
            
            const raycaster = new THREE.Raycaster(this.camera.position, direction);
            let endPoint = new THREE.Vector3().copy(this.camera.position).add(direction.clone().multiplyScalar(150));
            
            const intersects = raycaster.intersectObjects(this.scene.children, true);
            const validHits = intersects.filter(hit => hit.object !== this.localPlayer && hit.object !== this.firstPersonWeapon);
            if (validHits.length > 0) {
                endPoint = validHits[0].point;
                let normal = {x:0, y:1, z:0};
                if (validHits[0].face) {
                    const normVec = validHits[0].face.normal.clone().transformDirection(validHits[0].object.matrixWorld).normalize();
                    normal = {x: normVec.x, y: normVec.y, z: normVec.z};
                }
                this.spawnParticles(endPoint, normal, 'spark');
                this.spawnParticles(endPoint, normal, 'debris');
            }

            this.spawnTracer(startPoint, endPoint);
        }
    }

    spawnTracer(startPoint, endPoint) {
        const tracer = this.tracers.find(t => !t.active) || this.tracers[0];
        tracer.active = true;
        tracer.life = 0;
        tracer.startPos.copy(startPoint);
        tracer.endPos.copy(endPoint);
        tracer.mesh.visible = true;
        tracer.mesh.material.opacity = 1.0;
        const distance = startPoint.distanceTo(endPoint);
        const length = Math.min(distance, 5.0);
        tracer.mesh.scale.set(1, 1, length);
    }

    remoteFire(remote) {
        const startPoint = new THREE.Vector3();
        const ms = remote.mesh.getObjectByName("muzzleSocket");
        if (ms) {
            ms.getWorldPosition(startPoint);
        } else {
            remote.mesh.getWorldPosition(startPoint);
            startPoint.y += 1.3; 
        }
        
        const yaw = remote.mesh.rotation.y;
        const pitch = remote.mesh.userData.headPivot ? remote.mesh.userData.headPivot.rotation.x : 0;
        
        const direction = new THREE.Vector3(
            -Math.sin(yaw) * Math.cos(pitch),
            Math.sin(pitch),
            -Math.cos(yaw) * Math.cos(pitch)
        ).normalize();
        
        const raycaster = new THREE.Raycaster(startPoint, direction);
        let endPoint = new THREE.Vector3().copy(startPoint).add(direction.clone().multiplyScalar(100));
        
        const intersects = raycaster.intersectObjects(this.scene.children, true);
        const validHits = intersects.filter(hit => hit.object !== remote.mesh && hit.object !== this.localPlayer);
        if (validHits.length > 0) {
            endPoint = validHits[0].point;
            let normal = {x:0, y:1, z:0};
            if (validHits[0].face) {
                const normVec = validHits[0].face.normal.clone().transformDirection(validHits[0].object.matrixWorld).normalize();
                normal = {x: normVec.x, y: normVec.y, z: normVec.z};
            }
            this.spawnParticles(endPoint, normal, 'spark');
            this.spawnParticles(endPoint, normal, 'debris');
        }
        
        this.spawnTracer(startPoint, endPoint);
    }

    spawnParticles(position, normal = {x:0, y:1, z:0}, type = 'spark') {
        const count = type === 'spark' ? 12 : (type === 'debris' ? 6 : 4);
        const baseColor = type === 'spark' ? 0xffdd88 : (type === 'debris' ? 0x888888 : 0xaaaaaa);
        const normVec = new THREE.Vector3(normal.x, normal.y, normal.z).normalize();
        
        for (let i = 0; i < count; i++) {
            const p = this.particlePool.find(p => !p.active);
            if (!p) break;
            p.active = true;
            p.life = 0;
            p.maxLife = type === 'spark' ? 0.3 + Math.random() * 0.3 : 0.5 + Math.random() * 0.5;
            p.mesh.position.copy(position);
            p.mesh.visible = true;
            p.mesh.material.color.setHex(baseColor);
            p.mesh.material.opacity = 1.0;
            p.type = type;
            p.gravity = type === 'dust' ? 1.0 : 15.0;
            
            const spread = type === 'spark' ? 4.0 : 2.0;
            p.velocity.set(
                normVec.x * spread + (Math.random() - 0.5) * spread * 2,
                normVec.y * spread + (Math.random() - 0.5) * spread * 2 + (type === 'dust' ? 1 : 2),
                normVec.z * spread + (Math.random() - 0.5) * spread * 2
            );
            if (type === 'spark') p.velocity.multiplyScalar(1.5);
            
            p.mesh.scale.setScalar(type === 'dust' ? 3.0 : (type === 'debris' ? 1.5 : 0.6));
        }
    }

    updateLocalPlayer(position, rotation, stance) {
        if (this.localPlayer) {
            this.localPlayer.position.set(position.x, position.y, position.z);
            this.localPlayer.rotation.y = rotation.y;
            if (this.localPlayer.userData.headPivot) {
                this.localPlayer.userData.headPivot.rotation.x = rotation.x || 0;
            }
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

    updateRemotePlayers(players, localPlayerId, serverTick) {
        const activeIds = new Set(players.map((player) => player.playerId));
        this.remotePlayers.forEach((remote, playerId) => {
            if (!activeIds.has(playerId) || playerId === localPlayerId) {
                this.scene.remove(remote.mesh);
                remote.mesh.traverse((child) => {
                    if (child.isMesh) {
                        if (child.geometry) child.geometry.dispose();
                        if (child.material) {
                            if (Array.isArray(child.material)) {
                                child.material.forEach(m => m.dispose());
                            } else {
                                child.material.dispose();
                            }
                        }
                    }
                });
                this.remotePlayers.delete(playerId);
            }
        });
        players.forEach((player) => {
            if (player.playerId === localPlayerId) {
                return;
            }
            if (Number.isNaN(player.position.x) || Number.isNaN(player.position.y) || Number.isNaN(player.position.z) || player.position.y < 0) {
                return;
            }
            let remote = this.remotePlayers.get(player.playerId);
            if (!remote) {
                const teamColor = player.team === "TEAM_A" ? 0x66aaff : 0xff6666;
                remote = {
                    mesh: this.createPlayerModel(teamColor, player.currentWeapon),
                    snapshotBuffer: [],
                    stance: player.state || "STANDING",
                    weapon: player.currentWeapon
                };
                this.scene.add(remote.mesh);
                this.remotePlayers.set(player.playerId, remote);
            }
            if (remote.weapon !== player.currentWeapon) {
                this.scene.remove(remote.mesh);
                const teamColor = player.team === "TEAM_A" ? 0x66aaff : 0xff6666;
                remote.mesh = this.createPlayerModel(teamColor, player.currentWeapon);
                this.scene.add(remote.mesh);
                remote.weapon = player.currentWeapon;
            }
            remote.snapshotBuffer.push({
                serverTick: serverTick,
                timestamp: performance.now(),
                position: { ...player.position },
                rotation: { x: player.rotation.x || 0, y: player.rotation.y || 0 },
                stance: player.state
            });
            if (remote.snapshotBuffer.length > 20) {
                remote.snapshotBuffer.shift();
            }
        });
        this.raycastTargets = [...this.remotePlayers.entries()].map(([playerId, remote]) => ({
            playerId, mesh: remote.mesh
        }));
    }

    raycastOpponent() {
        const raycaster = new THREE.Raycaster();
        raycaster.setFromCamera(new THREE.Vector2(0, 0), this.camera);
        
        // Force update world matrices for accurate hitbox detection
        this.raycastTargets.forEach(target => target.mesh.updateMatrixWorld(true));
        
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

    getAimDirection() {
        const direction = new THREE.Vector3();
        this.camera.getWorldDirection(direction);
        return { x: direction.x, y: direction.y, z: direction.z };
    }

    showHitMarker(event) {
        const marker = document.querySelector("#hit-marker");
        if (!marker) {
            return;
        }
        marker.textContent = `${event.hitZone} ${event.damage} (${event.currentHp} HP)`;
        marker.hidden = false;
        window.clearTimeout(this.hitMarkerTimeout);
        this.hitMarkerTimeout = window.setTimeout(() => {
            marker.hidden = true;
        }, 700);
    }

    interpolateRemotePlayers(deltaSeconds) {
        const now = performance.now();
        const renderTime = now - 100;

        this.remotePlayers.forEach((remote) => {
            const buffer = remote.snapshotBuffer;
            if (!buffer || buffer.length === 0) return;

            while (buffer.length > 2 && buffer[1].timestamp <= renderTime) {
                buffer.shift();
            }

            const snap0 = buffer[0];
            const snap1 = buffer.length > 1 ? buffer[1] : buffer[0];
            const tickDelta = snap1.serverTick - snap0.serverTick;
            let speed = 0;
            
            if (snap1 !== snap0 && renderTime >= snap0.timestamp && renderTime <= snap1.timestamp) {
                const timeDiff = snap1.timestamp - snap0.timestamp;
                const alpha = timeDiff > 0 ? (renderTime - snap0.timestamp) / timeDiff : 1;
                const next = new THREE.Vector3().copy(snap0.position).lerp(snap1.position, alpha);
                speed = remote.mesh.position.distanceTo(next) / deltaSeconds;
                remote.mesh.position.copy(next);
                
                let r0 = snap0.rotation.y;
                let r1 = snap1.rotation.y;
                if (Math.abs(r1 - r0) > Math.PI) {
                    if (r1 > r0) r0 += Math.PI * 2;
                    else r1 += Math.PI * 2;
                }
                remote.mesh.rotation.y = THREE.MathUtils.lerp(r0, r1, alpha);
                if (remote.mesh.userData.headPivot) {
                    remote.mesh.userData.headPivot.rotation.x = THREE.MathUtils.lerp(snap0.rotation.x, snap1.rotation.x, alpha);
                }
                this.applyStance(remote.mesh, snap1.stance);
            } else if (renderTime > snap1.timestamp) {
                const next = new THREE.Vector3().copy(snap1.position);
                if (tickDelta > 0 && snap1 !== snap0) {
                    const extraTime = renderTime - snap1.timestamp;
                    if (extraTime < 150) {
                        const timeDiff = Math.max(1, snap1.timestamp - snap0.timestamp);
                        const velocity = new THREE.Vector3().subVectors(snap1.position, snap0.position).divideScalar(timeDiff);
                        next.add(velocity.multiplyScalar(extraTime));
                    }
                }
                speed = remote.mesh.position.distanceTo(next) / deltaSeconds;
                remote.mesh.position.copy(next);
                remote.mesh.rotation.y = snap1.rotation.y;
                if (remote.mesh.userData.headPivot) {
                    remote.mesh.userData.headPivot.rotation.x = snap1.rotation.x;
                }
                this.applyStance(remote.mesh, snap1.stance);
            } else {
                const next = new THREE.Vector3().copy(snap0.position);
                speed = remote.mesh.position.distanceTo(next) / deltaSeconds;
                remote.mesh.position.copy(next);
                remote.mesh.rotation.y = snap0.rotation.y;
                if (remote.mesh.userData.headPivot) {
                    remote.mesh.userData.headPivot.rotation.x = snap0.rotation.x;
                }
                this.applyStance(remote.mesh, snap0.stance);
            }

            if (speed > 0.5) {
                remote.mesh.userData.walkPhase = (remote.mesh.userData.walkPhase || 0) + speed * 1.5 * deltaSeconds;
            } else {
                remote.mesh.userData.walkPhase = THREE.MathUtils.lerp(remote.mesh.userData.walkPhase || 0, 0, 0.1);
            }
            
            const phase = remote.mesh.userData.walkPhase || 0;
            if (remote.mesh.userData.leftLegPivot) {
                remote.mesh.userData.leftLegPivot.rotation.x = Math.sin(phase) * 0.8;
                remote.mesh.userData.rightLegPivot.rotation.x = Math.sin(phase + Math.PI) * 0.8;
                remote.mesh.userData.leftArmPivot.rotation.x = -Math.PI / 2 + 0.4 + Math.sin(phase + Math.PI) * 0.3;
                remote.mesh.userData.rightArmPivot.rotation.x = -Math.PI / 2 + 0.2 + Math.sin(phase) * 0.3;
            }
        });
    }

    resetEntities() {
        this.remotePlayers.forEach(({ mesh }) => this.scene.remove(mesh));
        this.remotePlayers.clear();
        this.raycastTargets = [];
        if (this.localPlayer) {
            this.localPlayer.visible = false;
        }
        this.weaponRecoil = 0;
        this.setAiming(false);
        this.isReloading = false;
        this.reloadStartTime = 0;
        if (this.hitMarkerTimeout !== null) {
            window.clearTimeout(this.hitMarkerTimeout);
            this.hitMarkerTimeout = null;
        }
        const marker = document.querySelector("#hit-marker");
        if (marker) marker.hidden = true;
        
        if (this.particlePool) {
            for (const p of this.particlePool) {
                p.active = false;
                if (p.mesh) p.mesh.visible = false;
            }
        }
        if (this.tracers) {
            for (const t of this.tracers) {
                t.active = false;
                if (t.mesh) t.mesh.visible = false;
            }
        }
    }

    applyStance(model, stance) {
        const targetScaleY = {
            STANDING: 1.0,
            CROUCHING: 0.55,
            PRONE: 0.25,
            JUMPING: 1.05
        }[stance] || 1.0;
        
        const targetScaleZ = stance === "PRONE" ? 1.4 : 1.0;
        
        // Smoothly interpolate model proportions for an organic transition
        model.scale.y = THREE.MathUtils.lerp(model.scale.y, targetScaleY, 0.15);
        model.scale.z = THREE.MathUtils.lerp(model.scale.z, targetScaleZ, 0.15);
    }

    playReloadAnimation() {
        this.isReloading = true;
        this.reloadStartTime = performance.now();
    }

    render(deltaSeconds = 0.016) {
        // Update particles
        for (const p of this.particlePool) {
            if (p.active) {
                p.life += deltaSeconds;
                if (p.life >= p.maxLife) {
                    p.active = false;
                    p.mesh.visible = false;
                } else {
                    p.velocity.y -= p.gravity * deltaSeconds;
                    p.mesh.position.addScaledVector(p.velocity, deltaSeconds);
                    p.mesh.material.opacity = 1.0 - (p.life / p.maxLife);
                    p.mesh.rotation.x += p.velocity.z * deltaSeconds;
                    p.mesh.rotation.y += p.velocity.x * deltaSeconds;
                    if (p.type === 'dust') {
                        p.mesh.scale.setScalar(3.0 + (p.life / p.maxLife) * 3.0);
                    }
                }
            }
        }
        
        for (const t of this.tracers) {
            if (t.active) {
                t.life += deltaSeconds * 3.0; // Life goes 0 to 1
                if (t.life >= 1.0) {
                    t.active = false;
                    t.mesh.visible = false;
                } else {
                    const travelSpeed = 80;
                    const distance = t.startPos.distanceTo(t.endPos);
                    const currentDist = Math.min(distance, t.life * travelSpeed);
                    const alpha = distance > 0 ? currentDist / distance : 1;
                    
                    t.mesh.position.lerpVectors(t.startPos, t.endPos, alpha);
                    t.mesh.lookAt(t.endPos);
                    t.mesh.material.opacity = 1.0 - t.life;
                }
            }
        }

        const targetFov = this.aiming ? (this.currentWeaponType === 'BOLT_ACTION_RIFLE' ? 15 : 60) : 75;
        this.camera.fov = THREE.MathUtils.lerp(this.camera.fov, targetFov, 0.2);
        this.camera.updateProjectionMatrix();
        
        this.viewmodelCamera.fov = this.camera.fov;
        this.viewmodelCamera.updateProjectionMatrix();
        this.viewmodelCamera.quaternion.slerp(this.camera.quaternion, 15 * deltaSeconds);

        if (this.firstPersonWeapon) {
            try {
                const horizontalVelocity = Math.hypot(
                    this.camera.position.x - this.lastCameraPos.x,
                    this.camera.position.z - this.lastCameraPos.z
                );
                this.lastCameraPos.copy(this.camera.position);
                
                this.bobPhase += horizontalVelocity * 12;
                const bobX = Math.sin(this.bobPhase) * 0.015;
                const bobY = Math.abs(Math.cos(this.bobPhase)) * 0.015;

                const config = WEAPON_CONFIGS[this.currentWeaponType] || WEAPON_CONFIGS["ASSAULT_RIFLE"];
                
                let reloadRotX = 0;
                let reloadPosY = 0;
                let reloadPosZ = 0;

                if (this.isReloading) {
                    const t = (performance.now() - this.reloadStartTime) / 1000;
                    if (t >= 2.5) {
                        this.isReloading = false;
                    } else {
                        const magazine = this.firstPersonWeapon.getObjectByName("magazine");
                        const bolt = this.firstPersonWeapon.getObjectByName("bolt");
                        if (magazine && magazine.userData.baseY === undefined) magazine.userData.baseY = magazine.position.y;
                        if (bolt && bolt.userData.baseZ === undefined) bolt.userData.baseZ = bolt.position.z;

                        if (t < 0.4) {
                            const alpha = t / 0.4;
                            reloadRotX = THREE.MathUtils.lerp(0, Math.PI / 4, alpha);
                            reloadPosY = THREE.MathUtils.lerp(0, -0.1, alpha);
                            reloadPosZ = THREE.MathUtils.lerp(0, 0.1, alpha);
                        } else if (t < 1.2) {
                            reloadRotX = Math.PI / 4;
                            reloadPosY = -0.1;
                            reloadPosZ = 0.1;
                            if (magazine) {
                                const alpha = (t - 0.4) / 0.8;
                                magazine.position.y = THREE.MathUtils.lerp(magazine.userData.baseY, magazine.userData.baseY - 0.2, alpha);
                            }
                        } else if (t < 1.8) {
                            reloadRotX = Math.PI / 4;
                            reloadPosY = -0.1;
                            reloadPosZ = 0.1;
                            if (magazine) {
                                const alpha = (t - 1.2) / 0.6;
                                magazine.position.y = THREE.MathUtils.lerp(magazine.userData.baseY - 0.2, magazine.userData.baseY, alpha);
                            }
                        } else if (t < 2.2) {
                            reloadRotX = Math.PI / 4;
                            reloadPosY = -0.1;
                            reloadPosZ = 0.1;
                            if (bolt) {
                                const alpha = (t - 1.8) / 0.4;
                                const pull = alpha < 0.5 ? (alpha / 0.5) : (1 - (alpha - 0.5) / 0.5);
                                bolt.position.z = THREE.MathUtils.lerp(bolt.userData.baseZ, bolt.userData.baseZ + 0.05, pull);
                            }
                        } else {
                            const alpha = (t - 2.2) / 0.3;
                            reloadRotX = THREE.MathUtils.lerp(Math.PI / 4, 0, alpha);
                            reloadPosY = THREE.MathUtils.lerp(-0.1, 0, alpha);
                            reloadPosZ = THREE.MathUtils.lerp(0.1, 0, alpha);
                        }
                    }
                }

                // Protect against NaN
                if (!isNaN(bobX) && !isNaN(bobY)) {
                    const targetX = this.aiming ? config.adsPosition.x : config.position.x + bobX;
                    const targetY = this.aiming ? config.adsPosition.y : config.position.y + bobY + reloadPosY;
                    const targetZ = this.aiming ? config.adsPosition.z : config.position.z + reloadPosZ;
                    
                    this.firstPersonWeapon.position.x = THREE.MathUtils.lerp(
                        this.firstPersonWeapon.position.x, targetX, 0.2
                    );
                    this.firstPersonWeapon.position.y = THREE.MathUtils.lerp(
                        this.firstPersonWeapon.position.y, targetY, 0.2
                    );
                    this.firstPersonWeapon.position.z = THREE.MathUtils.lerp(
                        this.firstPersonWeapon.position.z, targetZ + (this.weaponRecoil || 0), 0.3
                    );
                }
                
                this.weaponRecoil *= 0.72;
                this.firstPersonWeapon.rotation.x = THREE.MathUtils.lerp(
                    this.firstPersonWeapon.rotation.x, (this.aiming ? -0.02 : 0) + reloadRotX, 0.2);
            } catch (err) {
                console.error("[THREE.js Error] Render loop viewmodel crash:", err);
            }
        }
        this.composer.render();
    }

    disposeMaterial(material) {
        if (!material) return;
        if (material.map) material.map.dispose();
        if (material.lightMap) material.lightMap.dispose();
        if (material.bumpMap) material.bumpMap.dispose();
        if (material.normalMap) material.normalMap.dispose();
        if (material.specularMap) material.specularMap.dispose();
        if (material.envMap) material.envMap.dispose();
        if (material.roughnessMap) material.roughnessMap.dispose();
        material.dispose();
    }

    dispose() {
        const disposeObject = (object) => {
            if (object.isMesh) {
                if (object.geometry) object.geometry.dispose();
                if (object.material) {
                    if (Array.isArray(object.material)) {
                        object.material.forEach(mat => this.disposeMaterial(mat));
                    } else {
                        this.disposeMaterial(object.material);
                    }
                }
            }
        };

        this.scene.traverse(disposeObject);
        this.viewmodelScene.traverse(disposeObject);

        this.scene.clear();
        this.viewmodelScene.clear();
        
        if (this.scene.background && this.scene.background.dispose) {
            this.scene.background.dispose();
        }

        this.materials = {};
        this.gunTemplates.clear();
        this.remotePlayers.clear();
        this.colliders = [];
        this.localPlayer = null;
        this.firstPersonWeapon = null;
        this.muzzleLight = null;
        this.muzzleSprite = null;
        
        this.addLighting();
    }

    resize() {
        const width = window.innerWidth;
        const height = window.innerHeight;
        if (width && height) {
            const pixelRatio = window.devicePixelRatio || 1;
            if (pixelRatio !== this.pixelRatio) {
                this.pixelRatio = pixelRatio;
                this.renderer.setPixelRatio(pixelRatio);
            }
            this.camera.aspect = width / height;
            this.camera.updateProjectionMatrix();
            if (this.viewmodelCamera) {
                this.viewmodelCamera.aspect = width / height;
                this.viewmodelCamera.updateProjectionMatrix();
            }
            this.renderer.setSize(width, height, false);
            if (this.composer) {
                this.composer.setSize(width, height);
            }
        }
    }
}
