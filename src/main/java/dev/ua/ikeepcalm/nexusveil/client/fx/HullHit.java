package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class HullHit extends Fx {
    private static final int[] SPARKS = {0xFFFFFF, 0xFFD27A, 0xFF8A2A};

    private final float roll = RANDOM.nextFloat() * (float) Math.PI * 2f;

    public HullHit(Vec3 at, Vec3 outward, int wood, double sea) {
        super(at);
        Vfx.add(new Streaks(at, outward, 22, 0.9, 0.9, 5f, 14f, 0.05f, 0.045f, SPARKS, true));
        Vfx.add(new Embers(at, outward, 14, 0.8, 0.8, 6f, 20f, 0.05f));
        Vfx.add(new Splinters(at, outward, wood, sea));
        for (int i = 0; i < 10; i++) {
            Vec3 velocity = outward.scale(0.1 + 0.25 * RANDOM.nextDouble()).add(spread(0.08));
            Vfx.add(new Smoke(at.add(spread(0.4)), velocity, 0.006f + 0.004f * RANDOM.nextFloat(), 0.25f + 0.2f * RANDOM.nextFloat(),
                    0.6f + 0.7f * RANDOM.nextFloat(), 80 + RANDOM.nextInt(60), 0x2E2B28, 0x7A7571, 0.5f, i < 3));
        }
    }

    @Override
    protected int lifetime() {
        return 8;
    }

    @Override
    public Identifier sheet() {
        return MuzzleBlast.FIRE;
    }

    @Override
    public boolean sheetGlows() {
        return true;
    }

    @Override
    public void sheet(FxDraw draw, float t) {
        float flash = fade(t, 0f, 5f);
        if (flash > 0f) {
            float u0 = Math.min(3, (int) (t / 1.25f)) / 4f;
            float size = 1.0f + 1.0f * easeOut(t / 4f);
            draw.billboardSprite(this.at, size, size, this.roll, u0, 0f, u0 + 0.25f, 0.5f, 0xFFFFFF, flash);
        }
    }

    @Override
    public void glow(FxDraw draw, float t) {
        float ring = (float) Math.pow(fade(t, 0f, 8f), 1.5);
        if (ring > 0f) {
            draw.billboardRing(this.at, 0.4f + 3.5f * easeOut(t / 8f), 0.22f, 0xFFE0B0, ring * 0.6f);
        }
    }
}
