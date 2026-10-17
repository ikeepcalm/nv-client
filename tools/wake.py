# Generates the pixel-art wake foam sheet: 4 variants across, 3 animation frames down, 24px cells.
# python tools/wake.py src/main/resources/assets/nexusveil-client/textures/fx/wake_foam.png [preview.png]
import random, sys
from PIL import Image

CELL = 24
VARIANTS = 4
FRAMES = 3
WHITE = (255, 255, 255, 255)
PALE = (191, 221, 245, 255)
SHADE = (143, 188, 230, 255)
SEA = (46, 99, 184, 255)


def blob(rng):
    cells = set()
    for _ in range(rng.randint(3, 5)):
        cx, cy = rng.uniform(8, 16), rng.uniform(9, 15)
        r = rng.uniform(2.5, 5.0)
        for y in range(CELL):
            for x in range(CELL):
                if (x - cx) ** 2 + (y - cy) ** 2 * 1.4 <= r * r:
                    cells.add((x, y))
    return cells


def frame(cells, rng):
    pixels = {}
    for (x, y) in cells:
        pixels[(x, y)] = SHADE if (x, y + 1) not in cells else (PALE if (x, y + 2) not in cells else WHITE)
    inner = [c for c in cells if all((c[0] + dx, c[1] + dy) in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for (x, y) in rng.sample(inner, min(len(inner), rng.randint(4, 7))):
        pixels[(x, y)] = PALE
        if rng.random() < 0.4:
            pixels[(x + 1, y)] = PALE
    for _ in range(rng.randint(2, 4)):
        x, y = rng.randint(1, CELL - 3), rng.randint(1, CELL - 3)
        if (x, y) in cells:
            continue
        size = rng.choice((1, 1, 2))
        for dx in range(size):
            for dy in range(size):
                pixels[(x + dx, y + dy)] = WHITE if size == 1 else PALE
    return pixels


sheet = Image.new("RGBA", (CELL * VARIANTS, CELL * FRAMES))
for v in range(VARIANTS):
    rng = random.Random(100 + v)
    cells = blob(rng)
    for f in range(FRAMES):
        for (x, y), colour in frame(cells, random.Random(1000 + v * 10 + f)).items():
            if 0 <= x < CELL and 0 <= y < CELL:
                sheet.putpixel((v * CELL + x, f * CELL + y), colour)
sheet.save(sys.argv[1])

if len(sys.argv) > 2:
    scale = 6
    preview = Image.new("RGBA", sheet.size, SEA)
    preview.alpha_composite(sheet)
    preview.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST).save(sys.argv[2])
