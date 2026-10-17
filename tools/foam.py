# Generates the foam sprite the client draws on the water. Rerun after changing anything below.
import math, random, sys
from PIL import Image

SIZE = 64
random.seed(7)
alpha = [[0.0] * SIZE for _ in range(SIZE)]

def put(x, y, a):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        alpha[y][x] = max(alpha[y][x], a)

for _ in range(16):
    cx, cy = random.gauss(SIZE / 2, SIZE / 7), random.gauss(SIZE / 2, SIZE / 7)
    r = random.uniform(2, 5)
    for y in range(int(cy - r), int(cy + r) + 1):
        for x in range(int(cx - r), int(cx + r) + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                put(x, y, random.uniform(0.7, 0.95))
for _ in range(120):
    cx, cy = random.uniform(4, SIZE - 4), random.uniform(4, SIZE - 4)
    r = random.choice([1.2, 1.6, 2.2, 2.8, 3.5, 4.5])
    for step in range(int(r * 8) + 8):
        t = 2 * math.pi * step / (int(r * 8) + 8)
        put(int(round(cx + math.cos(t) * r)), int(round(cy + math.sin(t) * r)), random.uniform(0.75, 1.0))
for _ in range(260):
    put(random.randrange(SIZE), random.randrange(SIZE), random.uniform(0.45, 0.9))
for _ in range(90):
    x, y = random.randrange(SIZE), random.randrange(SIZE)
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        put(x + dx, y + dy, random.uniform(0.3, 0.55))

image = Image.new("RGBA", (SIZE, SIZE))
lobes = [random.uniform(0.6, 1.0) for _ in range(11)]
for y in range(SIZE):
    for x in range(SIZE):
        dx, dy = (x + 0.5) / SIZE * 2 - 1, (y + 0.5) / SIZE * 2 - 1
        angle = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * len(lobes)
        i = int(angle) % len(lobes)
        f = angle - int(angle)
        edge = lobes[i] * (1 - f) + lobes[(i + 1) % len(lobes)] * f
        r = math.hypot(dx, dy) / edge
        falloff = max(0.0, 1 - r * r) ** 1.4
        a = alpha[y][x] * falloff
        base = 0.22 * falloff
        a = max(a, base)
        image.putpixel((x, y), (int(235 + 20 * (a > base)), 245, 255, int(255 * min(1.0, a))))
image.save(sys.argv[1])
