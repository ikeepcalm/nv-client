package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A pixel-art column of water with square droplets arcing off its crest and dashed rings running out across the sea.
public class WaterSplash extends Fx {
    private static final int[] DROPS = {0xFFFFFF, 0xDDEBFA, 0xBFD8F2};
    private static final Quaternionf UPRIGHT = new Quaternionf();
    private static final float COLUMN_TICKS = 30f;
    private static final float GRAVITY = 0.02f;
    private static final float LANDING_TICKS = 10f;

    private final float size;
    private final float yaw = RANDOM.nextFloat() * (float) Math.PI;
    private final Vec3[] origins;
    private final Vec3[] velocities;
    private final float[] launches;
    private final float[] landings;
    private final int[] colours;

    public WaterSplash(Vec3 surface, float size) {
        super(surface);
        this.size = size;
        int count = 6 + (int) (18 * size);
        this.origins = new Vec3[count];
        this.velocities = new Vec3[count];
        this.launches = new float[count];
        this.landings = new float[count];
        this.colours = new int[count];
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double reach = (0.05 + RANDOM.nextDouble() * 0.15) * Math.sqrt(size);
            this.origins[i] = surface.add(Math.cos(angle) * 0.6 * size, 1.8 * size + RANDOM.nextDouble() * size, Math.sin(angle) * 0.6 * size);
            this.velocities[i] = new Vec3(Math.cos(angle) * reach, (0.12 + RANDOM.nextDouble() * 0.18) * Math.sqrt(size), Math.sin(angle) * reach);
            this.launches[i] = 3f + RANDOM.nextFloat() * 7f;
            double vy = this.velocities[i].y;
            this.landings[i] = (float) ((vy + Math.sqrt(vy * vy + 2 * GRAVITY * (this.origins[i].y - surface.y))) / GRAVITY);
            this.colours[i] = DROPS[RANDOM.nextInt(DROPS.length)];
        }
        Vfx.add(new Foam(surface.add(0, 0.02, 0), Vec3.ZERO, 0.8f * size, 3f * size + 0.4f, 70, 0.75f));
    }

    @Override
    protected int lifetime() {
        return 56;
    }

    @Override
    public Identifier sheet() {
        return SplashSheet.TEXTURE;
    }

    @Override
    public void sheet(FxDraw draw, float t) {
        if (t < COLUMN_TICKS) {
            int frame = Math.min(SplashSheet.COLUMN_FRAMES - 1, (int) (t / COLUMN_TICKS * SplashSheet.COLUMN_FRAMES));
            SplashSheet.column(draw, this.at, this.yaw, 3f * this.size, frame, fade(t, 22f, COLUMN_TICKS));
        }
        Vec3 u = new Vec3(1, 0, 0);
        Vec3 v = new Vec3(0, 0, 1);
        for (int i = 0; i < 3; i++) {
            float p = (t - i * 7f) / 40f;
            if (p <= 0f || p >= 1f) {
                continue;
            }
            float r = (0.8f + 5f * easeOut(p)) * this.size + 0.3f;
            SplashSheet.ripple(draw, this.at.add(0, 0.03, 0), u, v, r, r, SplashSheet.rippleFrame(p), 0.9f * (float) Math.pow(1f - p, 1.2));
        }
        for (int i = 0; i < this.origins.length; i++) {
            float p = (t - this.launches[i] - this.landings[i]) / LANDING_TICKS;
            if (p <= 0f || p >= 1f) {
                continue;
            }
            Vec3 land = flight(this.origins[i], this.velocities[i], this.landings[i], 0f, GRAVITY);
            float r = (0.15f + 0.5f * easeOut(p)) * this.size + 0.1f;
            SplashSheet.ripple(draw, new Vec3(land.x, this.at.y + 0.03, land.z), u, v, r, r, SplashSheet.rippleFrame(p), 0.8f * (1f - p));
        }
    }

    @Override
    public void lit(FxDraw draw, float t) {
        float half = 0.04f + 0.08f * this.size;
        Vector3f halves = new Vector3f(half, half, half);
        for (int i = 0; i < this.origins.length; i++) {
            float flown = t - this.launches[i];
            if (flown <= 0f || flown >= this.landings[i]) {
                continue;
            }
            draw.box(flight(this.origins[i], this.velocities[i], flown, 0f, GRAVITY), halves, UPRIGHT, this.colours[i], 1f);
        }
    }
}
