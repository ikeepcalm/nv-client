# Generates the cannon flash sheet: row 0 is four frames of a muzzle burst, row 1 four frames of a flame tongue
# pointing +x. Usage: python tools/fire.py src/main/resources/assets/nexusveil-client/textures/fx/fire_flash.png
import math, random, sys
from PIL import Image

FRAME = 32
FRAMES = 4
random.seed(11)
PALETTE = [(255, 255, 255, 255), (255, 236, 150, 255), (255, 160, 48, 255), (214, 72, 16, 235), (90, 30, 10, 140)]

image = Image.new("RGBA", (FRAME * FRAMES, FRAME * 2), (0, 0, 0, 0))
spikes = [(1.0 if i % 2 == 0 else 0.55) * random.uniform(0.85, 1.0) for i in range(12)]


def band(v):
    if v <= 0:
        return None
    return PALETTE[min(len(PALETTE) - 1, int((1 - v) * len(PALETTE)))]


def burst(frame):
    grow = 0.72 + 0.09 * frame
    holes = 0.12 * frame
    core = 0.55 - 0.08 * frame
    for y in range(FRAME):
        for x in range(FRAME):
            dx, dy = (x + 0.5) / FRAME * 2 - 1, (y + 0.5) / FRAME * 2 - 1
            r = math.hypot(dx, dy)
            angle = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * len(spikes)
            i = int(angle) % len(spikes)
            f = angle - int(angle)
            reach = grow * (spikes[i] * (1 - f) + spikes[(i + 1) % len(spikes)] * f)
            v = max(1 - r / reach, 1 - r / (grow * core))
            if frame and random.random() < holes and v < 0.5:
                continue
            colour = band(v - 0.12 * frame)
            if colour:
                image.putpixel((frame * FRAME + x, y), colour)


def tongue(frame):
    wave = [random.uniform(-0.12, 0.12) for _ in range(FRAME)]
    length = 0.7 + 0.1 * frame
    for x in range(FRAME):
        u = (x + 0.5) / FRAME / length
        if u > 1:
            break
        half = (1 - u) ** 0.7 * 0.45 * (1 + wave[x]) + 0.02
        for y in range(FRAME):
            dy = (y + 0.5) / FRAME * 2 - 1
            v = (1 - abs(dy) / half) * (1 - u * 0.75) if abs(dy) < half else 0
            if frame >= 2 and random.random() < 0.1 * frame and u > 0.5:
                continue
            colour = band(v - 0.1 * frame)
            if colour:
                image.putpixel((frame * FRAME + x, FRAME + y), colour)


for frame in range(FRAMES):
    burst(frame)
    tongue(frame)
image.save(sys.argv[1])
