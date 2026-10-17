package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Glowing cinders from a gun or a hit: small cubes that tumble on an arc and cool from white to red before dying.
public class Embers extends Fx {
    private static final int[] HEAT = {0xFFF6D0, 0xFFD060, 0xFF8A2A, 0xD64810, 0x5A1E0A};

    private final Vec3[] velocities;
    private final float[] lives;
    private final float[] halves;
    private final Vector3f[] axes;
    private final Quaternionf[] bases;
    private final float gravity;
    private final int life;

    public Embers(Vec3 at, Vec3 direction, int count, double speed, double cone, float minLife, float maxLife, float gravity) {
        super(at);
        this.velocities = new Vec3[count];
        this.lives = new float[count];
        this.halves = new float[count];
        this.axes = new Vector3f[count];
        this.bases = new Quaternionf[count];
        for (int i = 0; i < count; i++) {
            this.velocities[i] = direction.add(spread(cone)).normalize().scale(speed * (0.3 + RANDOM.nextDouble() * 0.7));
            this.lives[i] = minLife + RANDOM.nextFloat() * (maxLife - minLife);
            this.halves[i] = 0.04f + RANDOM.nextFloat() * 0.07f;
            this.axes[i] = new Vector3f(RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f).normalize();
            this.bases[i] = new Quaternionf().rotateXYZ(RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f);
        }
        this.gravity = gravity;
        this.life = (int) Math.ceil(maxLife);
    }

    @Override
    protected int lifetime() {
        return this.life;
    }

    @Override
    public void glow(FxDraw draw, float t) {
        for (int i = 0; i < this.velocities.length; i++) {
            float p = t / this.lives[i];
            if (p >= 1f) {
                continue;
            }
            float h = this.halves[i] * (1f - 0.4f * p);
            draw.box(flight(this.at, this.velocities[i], t, 0.06f, this.gravity), new Vector3f(h, h, h),
                    new Quaternionf(this.bases[i]).rotateAxis(0.3f * t, this.axes[i]), HEAT[(int) (p * HEAT.length)], 1f - p * p);
        }
    }
}
