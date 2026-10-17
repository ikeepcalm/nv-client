# Generates the splash atlas: a row of column frames on top, a row of dashed ripple frames below.
# Rerun after changing anything below: python tools/splash.py <atlas.png> [preview.png]
import math, random, sys
from PIL import Image

FRAME_W, COL_H, RIP_H, COL_FRAMES, RIP_FRAMES = 32, 48, 32, 10, 4
ATLAS_W, ATLAS_H = FRAME_W * COL_FRAMES, COL_H + RIP_H
DARK, MID, LIGHT, GREY, WHITE = (46, 96, 176), (74, 128, 212), (128, 176, 232), (190, 206, 222), (242, 247, 252)
random.seed(11)
atlas = Image.new("RGBA", (ATLAS_W, ATLAS_H))


def smooth_noise(n, amplitude):
    raw = [random.uniform(-1, 1) for _ in range(n)]
    return [amplitude * (raw[i - 1] + 2 * raw[i] + raw[(i + 1) % n]) / 4 for i in range(n)]


def column_frame(f):
    p = f / (COL_FRAMES - 1)
    q = p ** 0.75
    envelope = max(0.12, math.sin(math.pi * q) ** 0.8)
    half = FRAME_W / 2 * (0.45 + 0.55 * min(1.0, q * 1.6 + 0.3 * p))
    heights = []
    spikes = smooth_noise(FRAME_W, 5.0)
    for x in range(FRAME_W):
        xn = (x + 0.5 - FRAME_W / 2) / half
        if abs(xn) >= 1:
            heights.append(0)
            continue
        dome = 0.3 + 0.7 * (1 - abs(xn) ** 1.6) * (1 - 0.25 * p)
        h = envelope * COL_H * 0.95 * dome + spikes[x] + (4 if random.random() < 0.12 else 0)
        heights.append(max(2, min(COL_H - 2, int(h))))
    wobble = smooth_noise(FRAME_W, 0.12)
    tongues = [random.choice((-0.18, 0.0, 0.0, 0.14)) for _ in range(FRAME_W)]
    foam = 0.2 + 0.5 * max(0.0, p - 0.4)
    holes = max(0.0, p - 0.55) * 0.6
    frame = Image.new("RGBA", (FRAME_W, COL_H))
    for x in range(FRAME_W):
        for row in range(heights[x]):
            d = row / max(1, heights[x]) + wobble[x] + tongues[x]
            if d > 1 - foam:
                colour = WHITE if d > 1 - foam * 0.6 or random.random() < 0.3 else GREY
            elif d > 0.62:
                colour = LIGHT
            elif d > 0.3:
                colour = MID
            else:
                colour = DARK
            if row > heights[x] * 0.5 and random.random() < holes:
                continue
            frame.putpixel((x, COL_H - 1 - row), colour + (255,))
    for _ in range(int(2 + 10 * math.sin(math.pi * min(1.0, p * 1.1)))):
        x = int(FRAME_W / 2 + random.uniform(-half, half) * 1.1)
        if 0 <= x < FRAME_W:
            y = COL_H - 1 - heights[max(0, min(FRAME_W - 1, x))] - random.randint(2, 9)
            size = random.choice((1, 1, 2))
            for dx in range(size):
                for dy in range(size):
                    if 0 <= x + dx < FRAME_W and 0 <= y + dy < COL_H:
                        frame.putpixel((x + dx, y + dy), WHITE + (255,))
    return frame


def ripple_frame(f):
    frame = Image.new("RGBA", (FRAME_W, RIP_H))
    radius = 5.5 + 3.0 * f
    dashes = 8 + 2 * f
    coverage = 0.7 - 0.12 * f
    centre = FRAME_W / 2
    for y in range(RIP_H):
        for x in range(FRAME_W):
            dx, dy = x + 0.5 - centre, y + 0.5 - centre
            r = math.hypot(dx, dy)
            if abs(r - radius) > 0.9:
                continue
            phase = ((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * dashes) % 1.0
            if phase < coverage:
                frame.putpixel((x, y), WHITE + (int(255 * (0.95 - 0.12 * f)),))
    return frame


for f in range(COL_FRAMES):
    atlas.paste(column_frame(f), (f * FRAME_W, 0))
for f in range(RIP_FRAMES):
    atlas.paste(ripple_frame(f), (f * FRAME_W, COL_H))
atlas.save(sys.argv[1])

if len(sys.argv) > 2:
    scale = 6
    preview = Image.new("RGBA", (ATLAS_W * scale, ATLAS_H * scale), (63, 118, 228, 255))
    preview.alpha_composite(atlas.resize((ATLAS_W * scale, ATLAS_H * scale), Image.NEAREST))
    preview.save(sys.argv[2])
