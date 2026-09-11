export class ArenaStrikeSoundManager {
    constructor() {
        this.context = null;
    }

    unlock() {
        if (!this.context) {
            this.context = new AudioContext();
        }
        if (this.context.state === "suspended") {
            return this.context.resume();
        }
        return Promise.resolve();
    }

    tone(startFrequency, endFrequency, duration, volume = 0.08, type = "square") {
        if (!this.context) {
            return;
        }
        const oscillator = this.context.createOscillator();
        const gain = this.context.createGain();
        const now = this.context.currentTime;
        oscillator.type = type;
        oscillator.frequency.setValueAtTime(startFrequency, now);
        oscillator.frequency.exponentialRampToValueAtTime(Math.max(20, endFrequency), now + duration);
        gain.gain.setValueAtTime(volume, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + duration);
        oscillator.connect(gain).connect(this.context.destination);
        oscillator.start(now);
        oscillator.stop(now + duration);
    }

    fire() {
        this.tone(150, 45, 0.12, 0.16);
        this.tone(900, 180, 0.035, 0.06, "sawtooth");
    }

    hit() {
        this.tone(220, 90, 0.08, 0.08, "triangle");
    }

    jump() {
        this.tone(180, 420, 0.12, 0.05, "sine");
    }

    kill() {
        this.tone(440, 880, 0.18, 0.08, "square");
    }

    stop() {
        if (this.context) {
            this.context.close();
            this.context = null;
        }
    }
}
