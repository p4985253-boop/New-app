import math
import os
import struct
import wave

SAMPLE_RATE = 44100

def write_wav(filename, samples):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with wave.open(filename, 'wb') as wav_file:
        wav_file.setnchannels(1)      # mono
        wav_file.setsampwidth(2)      # 16-bit
        wav_file.setframerate(SAMPLE_RATE)
        # Convert float samples (-1.0 to 1.0) to 16-bit integers
        int_samples = [max(-32767, min(32767, int(s * 32767.0))) for s in samples]
        data = struct.pack(f'<{len(int_samples)}h', *int_samples)
        wav_file.writeframes(data)

def generate_tone(freq, duration_sec, harmonics=[(1.0, 1.0), (2.0, 0.4), (3.0, 0.15)]):
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = []
    attack = int(SAMPLE_RATE * 0.02)
    decay = int(SAMPLE_RATE * 0.05)
    for i in range(num_samples):
        # ADSR Envelope
        if i < attack:
            env = i / attack
        elif i > num_samples - decay:
            env = (num_samples - i) / decay
        else:
            env = 1.0 - 0.3 * (i - attack) / (num_samples - attack - decay)
        
        # Audio sample
        t = i / SAMPLE_RATE
        val = 0.0
        for mult, amp in harmonics:
            val += amp * math.sin(2.0 * math.pi * (freq * mult) * t)
        samples.append(val * env * 0.35)
    return samples

def generate_chime():
    # Modern two-tone chime: Tone 1 (587.33 Hz / D5) -> Tone 2 (880 Hz / A5) with gentle sparkle
    samples = []
    t1_samples = int(SAMPLE_RATE * 0.14)
    for i in range(t1_samples):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 8.0) * min(1.0, i / 200.0)
        v = (math.sin(2.0 * math.pi * 587.33 * t) + 
             0.5 * math.sin(2.0 * math.pi * 1174.66 * t) + 
             0.2 * math.sin(2.0 * math.pi * 1762.0 * t))
        samples.append(v * env * 0.4)
    
    t2_samples = int(SAMPLE_RATE * 0.45)
    for i in range(t2_samples):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 5.0) * min(1.0, i / 150.0)
        v = (math.sin(2.0 * math.pi * 880.0 * t) + 
             0.4 * math.sin(2.0 * math.pi * 1760.0 * t) + 
             0.15 * math.sin(2.0 * math.pi * 2640.0 * t))
        samples.append(v * env * 0.45)
    return samples

def main():
    base_dir = "app/src/main/assets/audio"
    
    # 1. Chime
    chime_path = os.path.join(base_dir, "chime.wav")
    write_wav(chime_path, generate_chime())
    print("Generated chime.wav")

    # Vocabulary for English, Hindi, and Gujarati
    phrases = {
        "en": {
            "sent": 380,
            "rupees": 440,
            "and": 340,
            "hundred": 400,
            "thousand": 360,
            "lakh": 320
        },
        "hi": {
            "se": 360,
            "rupaye_prapt_hue": 420,
            "sau": 400,
            "hazaar": 350,
            "lakh": 310
        },
        "gu": {
            "tarafthi": 370,
            "rupiya_malya": 430,
            "so": 390,
            "hajar": 350,
            "lakh": 310
        }
    }

    # Generate phrase audio
    for lang, items in phrases.items():
        for name, pitch in items.items():
            f_path = os.path.join(base_dir, lang, f"{name}.wav")
            samples = generate_tone(pitch, 0.4)
            write_wav(f_path, samples)
            print(f"Generated {f_path}")

    # Numbers 1..20, tens (30..90)
    numbers = list(range(1, 21)) + [30, 40, 50, 60, 70, 80, 90]
    for lang in ["en", "hi", "gu"]:
        for n in numbers:
            f_path = os.path.join(base_dir, lang, f"{n}.wav")
            # Vary pitch slightly by number value for natural acoustic differentiation
            pitch = 300 + (n % 40) * 8
            samples = generate_tone(pitch, 0.32)
            write_wav(f_path, samples)
            
    print("All audio assets generated successfully!")

if __name__ == "__main__":
    main()
