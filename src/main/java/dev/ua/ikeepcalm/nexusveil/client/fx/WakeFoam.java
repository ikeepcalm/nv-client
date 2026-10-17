package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// A pixel-art foam blob lying on the sea, fixed in the world so the hull slides past it and leaves it as her trail.
public class WakeFoam extends Fx {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("nexusveil-client", "textures/fx/wake_foam.png");
    private static final int VARIANTS = 4;
    private static final int FRAMES = 3;
    private static final int FRAME_TICKS = 6;

    private final Vec3 drift;
    private final float radius;
    private final int life;
    private final float alpha;
    private final int variant = RANDOM.nextInt(VARIANTS);
    private final int turn = RANDOM.nextInt(4);

    public WakeFoam(Vec3 at, Vec3 drift, float radius, int life, float alpha) {
        super(at);
        this.drift = drift;
        this.radius = radius;
        this.life = life;
        this.alpha = alpha;
    }

    @Override
    protected int lifetime() {
        return this.life;
    }

    @Override
    public Identifier sheet() {
        return TEXTURE;
    }

    @Override
    public void sheet(FxDraw draw, float t) {
        float p = t / this.life;
        Vec3 centre = flight(this.at, this.drift, t, 0.06f, 0f);
        float a = this.alpha * Math.min(1f, t / 2f) * fade(p, 0.55f, 1f);
        float r = this.radius * (0.75f + 0.25f * easeOut(p));
        int frame = ((int) (t / FRAME_TICKS) + this.variant) % FRAMES;
        float u0 = (float) this.variant / VARIANTS;
        float v0 = (float) frame / FRAMES;
        Vec3[] corners = corners(centre, r);
        draw.quad(corners[this.turn], corners[(this.turn + 1) & 3], corners[(this.turn + 2) & 3], corners[(this.turn + 3) & 3],
                u0, v0, u0 + 1f / VARIANTS, v0 + 1f / FRAMES, 0xFFFFFF, a);
    }

    // Quarter turns only, so the pixels stay on the world's grid like everything else in the game.
    private static Vec3[] corners(Vec3 c, float r) {
        return new Vec3[]{c.add(-r, 0, -r), c.add(-r, 0, r), c.add(r, 0, r), c.add(r, 0, -r)};
    }
}
