import * as THREE from "https://cdn.jsdelivr.net/npm/three@0.168.0/build/three.module.js";

export function createTacticalSoldierModel(teamColorHex) {
    const root = new THREE.Group();
    
    // Materials
    const skinMaterial = new THREE.MeshStandardMaterial({ color: 0xd2b48c, roughness: 0.6 });
    const uniformMaterial = new THREE.MeshStandardMaterial({ color: teamColorHex, roughness: 0.8 }); // Team color for uniform
    const vestMaterial = new THREE.MeshStandardMaterial({ color: 0x222522, roughness: 0.9 }); // Dark olive/grey tactical vest
    const helmetMaterial = new THREE.MeshStandardMaterial({ color: 0x333833, roughness: 0.7 });
    const bootMaterial = new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.8 });
    const gogglesMaterial = new THREE.MeshStandardMaterial({ color: 0x111111, roughness: 0.2, metalness: 0.8 });

    // Helper to create and position meshes
    const createMesh = (geometry, material, name, hitLocation, castShadow = true) => {
        const mesh = new THREE.Mesh(geometry, material);
        mesh.name = name;
        mesh.castShadow = castShadow;
        mesh.receiveShadow = true;
        mesh.userData.hitLocation = hitLocation;
        return mesh;
    };

    // --- Pelvis (Root of the body structure) ---
    // The player's feet should be at y=0. Total height ~1.8m
    // Pelvis center around y=0.9
    const pelvis = new THREE.Group();
    pelvis.position.set(0, 0.9, 0);
    root.add(pelvis);

    const pelvisMesh = createMesh(new THREE.BoxGeometry(0.35, 0.2, 0.25), uniformMaterial, "pelvis", "TORSO");
    pelvis.add(pelvisMesh);

    // --- Torso ---
    const torsoPivot = new THREE.Group();
    torsoPivot.position.set(0, 0.1, 0); // Relative to pelvis
    pelvis.add(torsoPivot);

    const torsoMesh = createMesh(new THREE.BoxGeometry(0.4, 0.5, 0.25), uniformMaterial, "torso", "TORSO");
    torsoMesh.position.set(0, 0.25, 0);
    torsoPivot.add(torsoMesh);

    // Tactical Vest Layer
    const vestMesh = createMesh(new THREE.BoxGeometry(0.42, 0.45, 0.27), vestMaterial, "vest", "TORSO");
    vestMesh.position.set(0, 0.25, 0);
    torsoPivot.add(vestMesh);

    // --- Head & Helmet ---
    const headPivot = new THREE.Group();
    headPivot.position.set(0, 0.55, 0); // Relative to torso top
    torsoPivot.add(headPivot);
    root.userData.headPivot = headPivot;

    const headMesh = createMesh(new THREE.BoxGeometry(0.2, 0.22, 0.22), skinMaterial, "head", "HEAD");
    headMesh.position.set(0, 0.11, 0);
    headPivot.add(headMesh);

    const helmetMesh = createMesh(new THREE.BoxGeometry(0.22, 0.12, 0.24), helmetMaterial, "helmet", "HEAD");
    helmetMesh.position.set(0, 0.18, 0);
    headPivot.add(helmetMesh);

    const gogglesMesh = createMesh(new THREE.BoxGeometry(0.23, 0.06, 0.1), gogglesMaterial, "goggles", "HEAD");
    gogglesMesh.position.set(0, 0.12, 0.08);
    headPivot.add(gogglesMesh);

    // --- Arms ---
    // Left Arm
    const leftArmPivot = new THREE.Group();
    leftArmPivot.position.set(0.26, 0.45, 0);
    torsoPivot.add(leftArmPivot);
    root.userData.leftArmPivot = leftArmPivot;

    const leftUpperArm = createMesh(new THREE.BoxGeometry(0.12, 0.35, 0.12), uniformMaterial, "arm_l_upper", "LIMB");
    leftUpperArm.position.set(0, -0.15, 0);
    leftArmPivot.add(leftUpperArm);
    
    // Left Forearm
    const leftElbow = new THREE.Group();
    leftElbow.position.set(0, -0.32, 0);
    leftArmPivot.add(leftElbow);
    
    const leftForearm = createMesh(new THREE.BoxGeometry(0.1, 0.35, 0.1), skinMaterial, "arm_l_lower", "LIMB");
    leftForearm.position.set(0, -0.15, 0);
    leftElbow.add(leftForearm);

    // Right Arm
    const rightArmPivot = new THREE.Group();
    rightArmPivot.position.set(-0.26, 0.45, 0);
    torsoPivot.add(rightArmPivot);
    root.userData.rightArmPivot = rightArmPivot;

    const rightUpperArm = createMesh(new THREE.BoxGeometry(0.12, 0.35, 0.12), uniformMaterial, "arm_r_upper", "LIMB");
    rightUpperArm.position.set(0, -0.15, 0);
    rightArmPivot.add(rightUpperArm);

    // Right Forearm
    const rightElbow = new THREE.Group();
    rightElbow.position.set(0, -0.32, 0);
    rightArmPivot.add(rightElbow);
    
    const rightForearm = createMesh(new THREE.BoxGeometry(0.1, 0.35, 0.1), skinMaterial, "arm_r_lower", "LIMB");
    rightForearm.position.set(0, -0.15, 0);
    rightElbow.add(rightForearm);

    // Right Hand Socket
    const rightHandSocket = new THREE.Group();
    rightHandSocket.name = "RightHandSocket";
    rightHandSocket.position.set(0, -0.2, 0.15); // Adjust so weapon points forward
    rightElbow.add(rightHandSocket);

    // Pose arms for holding weapon
    rightArmPivot.rotation.set(-Math.PI / 2 + 0.2, 0, -0.2);
    leftArmPivot.rotation.set(-Math.PI / 2 + 0.4, 0, 0.3);
    leftElbow.rotation.set(-0.4, 0.5, 0);

    // --- Legs ---
    // Left Leg
    const leftLegPivot = new THREE.Group();
    leftLegPivot.position.set(0.12, -0.1, 0); // From pelvis
    pelvis.add(leftLegPivot);
    root.userData.leftLegPivot = leftLegPivot;

    const leftThigh = createMesh(new THREE.BoxGeometry(0.15, 0.45, 0.15), uniformMaterial, "leg_l_upper", "LIMB");
    leftThigh.position.set(0, -0.2, 0);
    leftLegPivot.add(leftThigh);

    const leftKnee = new THREE.Group();
    leftKnee.position.set(0, -0.42, 0);
    leftLegPivot.add(leftKnee);

    const leftCalf = createMesh(new THREE.BoxGeometry(0.13, 0.4, 0.13), uniformMaterial, "leg_l_lower", "LIMB");
    leftCalf.position.set(0, -0.2, 0);
    leftKnee.add(leftCalf);

    const leftBoot = createMesh(new THREE.BoxGeometry(0.15, 0.12, 0.2), bootMaterial, "boot_l", "LIMB");
    leftBoot.position.set(0, -0.43, 0.02);
    leftKnee.add(leftBoot);

    // Right Leg
    const rightLegPivot = new THREE.Group();
    rightLegPivot.position.set(-0.12, -0.1, 0); // From pelvis
    pelvis.add(rightLegPivot);
    root.userData.rightLegPivot = rightLegPivot;

    const rightThigh = createMesh(new THREE.BoxGeometry(0.15, 0.45, 0.15), uniformMaterial, "leg_r_upper", "LIMB");
    rightThigh.position.set(0, -0.2, 0);
    rightLegPivot.add(rightThigh);

    const rightKnee = new THREE.Group();
    rightKnee.position.set(0, -0.42, 0);
    rightLegPivot.add(rightKnee);

    const rightCalf = createMesh(new THREE.BoxGeometry(0.13, 0.4, 0.13), uniformMaterial, "leg_r_lower", "LIMB");
    rightCalf.position.set(0, -0.2, 0);
    rightKnee.add(rightCalf);

    const rightBoot = createMesh(new THREE.BoxGeometry(0.15, 0.12, 0.2), bootMaterial, "boot_r", "LIMB");
    rightBoot.position.set(0, -0.43, 0.02);
    rightKnee.add(rightBoot);

    root.userData.walkPhase = Math.random() * Math.PI * 2;
    return root;
}
