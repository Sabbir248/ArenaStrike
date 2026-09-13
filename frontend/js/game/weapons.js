import * as THREE from "https://cdn.jsdelivr.net/npm/three@0.168.0/build/three.module.js";

const createMaterial = (colorHex, roughness = 0.7, metalness = 0.5) => {
    return new THREE.MeshStandardMaterial({ color: colorHex, roughness, metalness });
};

const blackPolymer = createMaterial(0x1a1a1a, 0.8, 0.2);
const darkSteel = createMaterial(0x2b2b2b, 0.6, 0.8);
const lightSteel = createMaterial(0x666666, 0.5, 0.9);
const woodMat = createMaterial(0x5c4033, 0.9, 0.1);
const odGreen = createMaterial(0x4b5320, 0.9, 0.1);

function addMesh(group, geometry, material, x, y, z, rx = 0, ry = 0, rz = 0) {
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(x, y, z);
    mesh.rotation.set(rx, ry, rz);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    group.add(mesh);
    return mesh;
}

export function createDesertEagle() {
    const root = new THREE.Group();
    // Grip at origin
    addMesh(root, new THREE.BoxGeometry(0.04, 0.1, 0.06), blackPolymer, 0, -0.05, 0, 0.2, 0, 0);
    // Frame
    addMesh(root, new THREE.BoxGeometry(0.04, 0.04, 0.2), darkSteel, 0, 0.02, -0.05);
    // Slide / Barrel (Chrome/Light Steel)
    addMesh(root, new THREE.BoxGeometry(0.045, 0.045, 0.22), lightSteel, 0, 0.06, -0.06).name = "bolt";
    // Trigger Guard
    addMesh(root, new THREE.BoxGeometry(0.01, 0.05, 0.04), darkSteel, 0, -0.02, -0.06, 0.4, 0, 0);
    // Hammer
    addMesh(root, new THREE.BoxGeometry(0.01, 0.02, 0.02), darkSteel, 0, 0.04, 0.05, -0.5, 0, 0);
    // Magazine
    addMesh(root, new THREE.BoxGeometry(0.035, 0.1, 0.05), darkSteel, 0, -0.05, 0, 0.2, 0, 0).name = "magazine";

    const muzzleSocket = new THREE.Group();
    muzzleSocket.name = "muzzleSocket";
    muzzleSocket.position.set(0, 0.06, -0.17);
    root.add(muzzleSocket);
    
    return root;
}

export function createAKM() {
    const root = new THREE.Group();
    // Grip
    addMesh(root, new THREE.BoxGeometry(0.04, 0.12, 0.06), blackPolymer, 0, -0.06, 0, 0.2);
    // Receiver
    addMesh(root, new THREE.BoxGeometry(0.05, 0.07, 0.3), darkSteel, 0, 0.04, -0.1);
    // Stock (Wood)
    addMesh(root, new THREE.BoxGeometry(0.045, 0.1, 0.25), woodMat, 0, 0, 0.18, 0.1);
    // Handguard (Wood)
    addMesh(root, new THREE.BoxGeometry(0.055, 0.06, 0.15), woodMat, 0, 0.03, -0.3);
    // Barrel
    addMesh(root, new THREE.CylinderGeometry(0.012, 0.012, 0.3), darkSteel, 0, 0.05, -0.4, Math.PI/2);
    // Gas Block
    addMesh(root, new THREE.CylinderGeometry(0.01, 0.01, 0.15), darkSteel, 0, 0.07, -0.3, Math.PI/2);
    // Magazine (Curved Banana)
    addMesh(root, new THREE.BoxGeometry(0.03, 0.18, 0.08), darkSteel, 0, -0.08, -0.15, -0.2).name = "magazine";
    // Charging Handle
    addMesh(root, new THREE.CylinderGeometry(0.008, 0.008, 0.04), darkSteel, 0.025, 0.05, -0.1, 0, 0, Math.PI/2).name = "bolt";
    // Front Sight
    addMesh(root, new THREE.BoxGeometry(0.01, 0.03, 0.02), darkSteel, 0, 0.07, -0.52);

    const muzzleSocket = new THREE.Group();
    muzzleSocket.name = "muzzleSocket";
    muzzleSocket.position.set(0, 0.05, -0.55);
    root.add(muzzleSocket);

    return root;
}

export function createUMP() {
    const root = new THREE.Group();
    // Grip
    addMesh(root, new THREE.BoxGeometry(0.04, 0.1, 0.06), blackPolymer, 0, -0.05, 0, 0.1);
    // Receiver (Polymer)
    addMesh(root, new THREE.BoxGeometry(0.05, 0.08, 0.35), blackPolymer, 0, 0.04, -0.12);
    // Stock (Folding/Wire style)
    addMesh(root, new THREE.BoxGeometry(0.02, 0.08, 0.25), blackPolymer, 0, 0.04, 0.18);
    // Barrel
    addMesh(root, new THREE.CylinderGeometry(0.012, 0.012, 0.15), darkSteel, 0, 0.04, -0.35, Math.PI/2);
    // Magazine (Straight Stick)
    addMesh(root, new THREE.BoxGeometry(0.03, 0.2, 0.05), blackPolymer, 0, -0.1, -0.15).name = "magazine";
    // Charging Handle
    addMesh(root, new THREE.CylinderGeometry(0.008, 0.008, 0.03), lightSteel, 0.025, 0.06, -0.15, 0, 0, Math.PI/2).name = "bolt";
    // Top Rail / Sight
    addMesh(root, new THREE.BoxGeometry(0.03, 0.02, 0.3), darkSteel, 0, 0.09, -0.12);

    const muzzleSocket = new THREE.Group();
    muzzleSocket.name = "muzzleSocket";
    muzzleSocket.position.set(0, 0.04, -0.42);
    root.add(muzzleSocket);

    return root;
}

export function createS686() {
    const root = new THREE.Group();
    // Grip (integrated into stock)
    addMesh(root, new THREE.BoxGeometry(0.045, 0.1, 0.08), woodMat, 0, -0.04, 0, 0.3);
    // Receiver (Steel)
    addMesh(root, new THREE.BoxGeometry(0.05, 0.06, 0.15), lightSteel, 0, 0.03, -0.08);
    // Stock (Wood)
    addMesh(root, new THREE.BoxGeometry(0.05, 0.12, 0.3), woodMat, 0, -0.02, 0.18, 0.1);
    // Double Barrels (Side-by-side)
    const barrelGroup = new THREE.Group();
    barrelGroup.name = "magazine"; // Reusing magazine name for barrel hinge
    barrelGroup.position.set(0, 0.04, -0.08); // Hinge origin
    addMesh(barrelGroup, new THREE.CylinderGeometry(0.015, 0.015, 0.6), darkSteel, 0.015, 0, -0.37, Math.PI/2);
    addMesh(barrelGroup, new THREE.CylinderGeometry(0.015, 0.015, 0.6), darkSteel, -0.015, 0, -0.37, Math.PI/2);
    addMesh(barrelGroup, new THREE.BoxGeometry(0.06, 0.04, 0.25), woodMat, 0, -0.02, -0.2);
    root.add(barrelGroup);

    const muzzleSocket = new THREE.Group();
    muzzleSocket.name = "muzzleSocket";
    muzzleSocket.position.set(0, 0.04, -0.75);
    root.add(muzzleSocket);

    return root;
}

export function createAWM() {
    const root = new THREE.Group();
    // Grip
    addMesh(root, new THREE.BoxGeometry(0.04, 0.1, 0.06), blackPolymer, 0, -0.05, 0, 0.2);
    // Chassis / Body (OD Green)
    addMesh(root, new THREE.BoxGeometry(0.06, 0.08, 0.4), odGreen, 0, 0.04, -0.15);
    // Stock (OD Green)
    addMesh(root, new THREE.BoxGeometry(0.05, 0.12, 0.3), odGreen, 0, 0.02, 0.2);
    // Cheek Rest
    addMesh(root, new THREE.BoxGeometry(0.06, 0.03, 0.15), blackPolymer, 0, 0.09, 0.18);
    // Heavy Barrel
    addMesh(root, new THREE.CylinderGeometry(0.015, 0.018, 0.6), darkSteel, 0, 0.05, -0.6, Math.PI/2);
    // Muzzle Brake
    addMesh(root, new THREE.CylinderGeometry(0.025, 0.025, 0.08), darkSteel, 0, 0.05, -0.9, Math.PI/2);
    // Box Magazine
    addMesh(root, new THREE.BoxGeometry(0.04, 0.12, 0.08), darkSteel, 0, -0.06, -0.15).name = "magazine";
    // Scope (Detailed)
    addMesh(root, new THREE.CylinderGeometry(0.025, 0.025, 0.25), blackPolymer, 0, 0.12, -0.1, Math.PI/2);
    addMesh(root, new THREE.CylinderGeometry(0.035, 0.025, 0.05), blackPolymer, 0, 0.12, -0.25, Math.PI/2); // Scope bell
    addMesh(root, new THREE.BoxGeometry(0.02, 0.04, 0.04), blackPolymer, 0, 0.09, -0.05); // Mount 1
    addMesh(root, new THREE.BoxGeometry(0.02, 0.04, 0.04), blackPolymer, 0, 0.09, -0.15); // Mount 2
    // Bolt Handle (Right side)
    const boltGroup = new THREE.Group();
    boltGroup.name = "bolt";
    addMesh(boltGroup, new THREE.CylinderGeometry(0.008, 0.008, 0.05), lightSteel, 0.04, 0.05, -0.02, 0, 0, Math.PI/2);
    addMesh(boltGroup, new THREE.SphereGeometry(0.015), blackPolymer, 0.06, 0.05, -0.02);
    root.add(boltGroup);

    const muzzleSocket = new THREE.Group();
    muzzleSocket.name = "muzzleSocket";
    muzzleSocket.position.set(0, 0.05, -0.94);
    root.add(muzzleSocket);

    return root;
}
