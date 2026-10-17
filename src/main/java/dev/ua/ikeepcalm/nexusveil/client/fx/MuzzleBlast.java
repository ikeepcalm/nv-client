package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class MuzzleBlast extends Fx {
    static final Identifier FIRE = Identifier.fromNamespaceAndPath("nexusveil-client", "textures/fx/fire_flash.png");
    private static final int[] SPARKS = {0xFFF2C4, 0xFFB347, 0xFF7A1F};

    private final Vec3 bore;
    private final Vec3 across;
    private final Vec3 over;
    private final float roll = RANDOM.nextFloat() * (float) Math.PI * 2f;

    public MuzzleBlast(Vec3 muzzle, Vec3 bore) {
        super(muzzle.add(bore.scale(0.6)));
        this.bore = bore;
        Vec3 side = bore.cross(new Vec3(0, 1, 0));
        this.across = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
        this.over = this.across.cross(bore).normalize();

        for (int i = 0; i < 30; i++) {
            Vec3 origin = this.at.add(bore.scale(RANDOM.nextDouble() * 2.0)).add(spread(0.3));
            Vec3 velocity = bore.scale(0.15 + 0.8 * RANDOM.nextDouble()).add(spread(0.2));
            Vfx.add(new Smoke(origin, velocity, 0.003f + 0.005f * RANDOM.nextFloat(), 0.25f + 0.25f * RANDOM.nextFloat(),
                    0.6f + RANDOM.nextFloat(), 120 + RANDOM.nextInt(100), 0x5C5955, 0xE6E3DE, 0.38f, i < 8));
        }
        Vfx.add(new Embers(this.at, bore, 20, 1.0, 0.3, 8f, 26f, 0.03f));
        Vfx.add(new Streaks(this.at, bore, 12, 1.2, 0.3, 5f, 12f, 0.03f, 0.04f, SPARKS, true));
    }

    @Override
    protected int lifetime() {
        return 12;
    }

    @Override
    public Identifier sheet() {
        return FIRE;
    }

    @Override
    public boolean sheetGlows() {
        return true;
    }

    @Override
    public void sheet(FxDraw draw, float t) {
        float flash = fade(t, 0f, 6f);
        if (flash <= 0f) {
            return;
        }
        float u0 = Math.min(3, (int) (t / 1.5f)) / 4f;
        float size = 1.6f + 1.2f * easeOut(t / 4f);
        draw.billboardSprite(this.at.add(this.bore.scale(0.4)), size, size, this.roll, u0, 0f, u0 + 0.25f, 0.5f, 0xFFFFFF, flash);
        Vec3 tip = this.at.add(this.bore.scale(4.5f * easeOut(t / 2f)));
        tongue(draw, tip, this.across.scale(1.1), u0, flash);
        tongue(draw, tip, this.over.scale(1.1), u0, flash);
    }

    private void tongue(FxDraw draw, Vec3 tip, Vec3 width, float u0, float alpha) {
        draw.quad(this.at.add(width), this.at.subtract(width), tip.subtract(width), tip.add(width),
                u0, 0.5f, u0 + 0.25f, 1f, 0xFFFFFF, alpha);
    }

    @Override
    public void glow(FxDraw draw, float t) {
        float ring = (float) Math.pow(fade(t, 0f, 7f), 1.5);
        if (ring > 0f) {
            draw.billboardRing(this.at.add(this.bore), 0.6f + 5.0f * easeOut(t / 7f), 0.25f, 0xFFFFFF, ring * 0.4f);
        }
    }
}
