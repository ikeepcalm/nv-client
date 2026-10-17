package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;

// A spray of sparks or droplets from one point, each on its own analytic arc.
public class Streaks extends Fx {
    private final Vec3[] velocities;
    private final float[] lives;
    private final int[] colours;
    private final float gravity;
    private final float width;
    private final boolean glowing;
    private final int life;

    public Streaks(Vec3 at, Vec3 direction, int count, double speed, double cone, float minLife, float maxLife,
                   float gravity, float width, int[] palette, boolean glowing) {
        super(at);
        this.velocities = new Vec3[count];
        this.lives = new float[count];
        this.colours = new int[count];
        for (int i = 0; i < count; i++) {
            this.velocities[i] = direction.add(spread(cone)).normalize().scale(speed * (0.4 + RANDOM.nextDouble() * 0.6));
            this.lives[i] = minLife + RANDOM.nextFloat() * (maxLife - minLife);
            this.colours[i] = palette[RANDOM.nextInt(palette.length)];
        }
        this.gravity = gravity;
        this.width = width;
        this.glowing = glowing;
        this.life = (int) Math.ceil(maxLife);
    }

    @Override
    protected int lifetime() {
        return this.life;
    }

    @Override
    public void glow(FxDraw draw, float t) {
        if (this.glowing) {
            draw(draw, t);
        }
    }

    @Override
    public void lit(FxDraw draw, float t) {
        if (!this.glowing) {
            draw(draw, t);
        }
    }

    private void draw(FxDraw draw, float t) {
        for (int i = 0; i < this.velocities.length; i++) {
            if (t >= this.lives[i]) {
                continue;
            }
            Vec3 head = flight(this.at, this.velocities[i], t, 0.08f, this.gravity);
            Vec3 tail = flight(this.at, this.velocities[i], Math.max(0f, t - 1.5f), 0.08f, this.gravity);
            float a = (float) Math.pow(1f - t / this.lives[i], 0.8);
            draw.streak(head, tail, this.width * (1f - 0.5f * t / this.lives[i]), this.colours[i], a);
        }
    }
}
