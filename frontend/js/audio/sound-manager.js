export class ArenaStrikeSoundManager {
    constructor() {
        this.context = null;
        this.buffers = null;
    }

    unlock() {
        if (!this.context) {
            this.context = new (window.AudioContext || window.webkitAudioContext)();
            this.listener = this.context.listener;
        }
        if (this.context.state === "suspended") {
            return this.context.resume();
        }
        return Promise.resolve();
    }

    updateListener(position, rotation) {
        if (!this.context || !this.listener) return;
        
        const time = this.context.currentTime + 0.05;
        if (this.listener.positionX) {
            this.listener.positionX.linearRampToValueAtTime(position.x, time);
            this.listener.positionY.linearRampToValueAtTime(position.y, time);
            this.listener.positionZ.linearRampToValueAtTime(position.z, time);
        } else {
            this.listener.setPosition(position.x, position.y, position.z);
        }
        
        const pitch = rotation.x;
        const yaw = rotation.y;
        
        const forwardX = -Math.sin(yaw) * Math.cos(pitch);
        const forwardY = Math.sin(pitch);
        const forwardZ = -Math.cos(yaw) * Math.cos(pitch);
        
        const upX = Math.sin(yaw) * Math.sin(pitch);
        const upY = Math.cos(pitch);
        const upZ = Math.cos(yaw) * Math.sin(pitch);

        if (this.listener.forwardX) {
            this.listener.forwardX.linearRampToValueAtTime(forwardX, time);
            this.listener.forwardY.linearRampToValueAtTime(forwardY, time);
            this.listener.forwardZ.linearRampToValueAtTime(forwardZ, time);
            this.listener.upX.linearRampToValueAtTime(upX, time);
            this.listener.upY.linearRampToValueAtTime(upY, time);
            this.listener.upZ.linearRampToValueAtTime(upZ, time);
        } else {
            this.listener.setOrientation(forwardX, forwardY, forwardZ, upX, upY, upZ);
        }
    }

    createGunshotBuffer() {
        const sr = this.context.sampleRate;
        const length = sr * 0.8;
        const buffer = this.context.createBuffer(1, length, sr);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < length; i++) {
            const t = i / sr;
            const env = Math.exp(-t * 30);
            const noise = (Math.random() * 2 - 1) * env * 0.7;
            
            const boltEnv = t > 0.05 ? Math.exp(-(t - 0.05) * 50) : 0;
            const boltNoise = (Math.random() * 2 - 1) * boltEnv * 0.4;
            
            const reverbEnv = Math.exp(-t * 4);
            const reverbNoise = (Math.random() * 2 - 1) * reverbEnv * 0.15;
            
            data[i] = noise + boltNoise + reverbNoise;
        }
        return buffer;
    }

    createReloadBuffer() {
        const sr = this.context.sampleRate;
        const length = sr * 1.5;
        const buffer = this.context.createBuffer(1, length, sr);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < length; i++) {
            const t = i / sr;
            const magOut = t > 0.1 && t < 0.2 ? Math.exp(-(t - 0.1) * 30) * (Math.random() * 2 - 1) * 0.3 : 0;
            const magIn = t > 0.8 && t < 0.9 ? Math.exp(-(t - 0.8) * 40) * (Math.random() * 2 - 1) * 0.5 : 0;
            const boltPull = t > 1.3 && t < 1.4 ? Math.exp(-(t - 1.3) * 50) * (Math.random() * 2 - 1) * 0.6 : 0;
            data[i] = magOut + magIn + boltPull;
        }
        return buffer;
    }

    createDryFireBuffer() {
        const sr = this.context.sampleRate;
        const length = sr * 0.1;
        const buffer = this.context.createBuffer(1, length, sr);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < length; i++) {
            const t = i / sr;
            const env = Math.exp(-t * 80);
            data[i] = (Math.random() * 2 - 1) * env * 0.4;
        }
        return buffer;
    }

    getBuffer(name) {
        if (!this.buffers) {
            this.buffers = {
                gunshot: this.createGunshotBuffer(),
                reload: this.createReloadBuffer(),
                dryFire: this.createDryFireBuffer()
            };
        }
        return this.buffers[name];
    }

    playBuffer(name, position = null) {
        if (!this.context) return;
        const source = this.context.createBufferSource();
        source.buffer = this.getBuffer(name);
        source.playbackRate.value = 0.95 + Math.random() * 0.1;
        
        let targetNode = this.context.destination;

        if (position) {
            const panner = this.context.createPanner();
            panner.panningModel = 'HRTF';
            panner.distanceModel = 'inverse';
            panner.refDistance = 1;
            panner.maxDistance = 100;
            panner.rolloffFactor = 1;
            
            panner.positionX.value = position.x;
            panner.positionY.value = position.y;
            panner.positionZ.value = position.z;
            
            panner.connect(this.context.destination);
            targetNode = panner;
        }
        
        source.connect(targetNode);
        source.start(0);
    }

    fire(position = null) {
        this.playBuffer("gunshot", position);
    }

    empty() {
        this.playBuffer("dryFire");
    }

    reload() {
        this.playBuffer("reload");
    }

    hit() {
        // Simple oscillator tone for hitmarker (still useful)
        if (!this.context) return;
        const oscillator = this.context.createOscillator();
        const gain = this.context.createGain();
        const now = this.context.currentTime;
        oscillator.type = "triangle";
        oscillator.frequency.setValueAtTime(220, now);
        oscillator.frequency.exponentialRampToValueAtTime(90, now + 0.08);
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);
        oscillator.connect(gain).connect(this.context.destination);
        oscillator.start(now);
        oscillator.stop(now + 0.08);
    }

    jump() {
        if (!this.context) return;
        const oscillator = this.context.createOscillator();
        const gain = this.context.createGain();
        const now = this.context.currentTime;
        oscillator.type = "sine";
        oscillator.frequency.setValueAtTime(180, now);
        oscillator.frequency.exponentialRampToValueAtTime(420, now + 0.12);
        gain.gain.setValueAtTime(0.05, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);
        oscillator.connect(gain).connect(this.context.destination);
        oscillator.start(now);
        oscillator.stop(now + 0.12);
    }

    kill() {
        if (!this.context) return;
        const oscillator = this.context.createOscillator();
        const gain = this.context.createGain();
        const now = this.context.currentTime;
        oscillator.type = "square";
        oscillator.frequency.setValueAtTime(440, now);
        oscillator.frequency.exponentialRampToValueAtTime(880, now + 0.18);
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);
        oscillator.connect(gain).connect(this.context.destination);
        oscillator.start(now);
        oscillator.stop(now + 0.18);
    }

    stop() {
        if (this.context) {
            this.context.close();
            this.context = null;
        }
    }
}
