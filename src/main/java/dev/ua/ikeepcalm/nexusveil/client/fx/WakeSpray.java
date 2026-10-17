package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Square droplets thrown off the stem, each a small cube on its own arc that vanishes where it meets the sea.
public class WakeSpray extends Fx {
    private static final Quaternionf UPRIGHT = new Quaternionf();
    private static final int[] COLOURS = {0xFFFFFF, 0xFFFFFF, 0xDDEBFA};
    private static final int LIFE = 24;

    private final Vec3[] velocities;
    private final float[] halves;
    private final int[] colours;
    private final double surface;

    public WakeSpray(Vec3 at, Vec3 direction, int count, double speed, double cone, double surface) {
        super(at);
        this.surface = surface;
        this.velocities = new Vec3[count];
        this.halves = new float[count];
        this.colours = new int[count];
        for (int i = 0; i < count; i++) {
            this.velocities[i] = direction.add(spread(cone)).normalize().scale(speed * (0.5 + RANDOM.nextDouble() * 0.5));
            this.halves[i] = 0.05f + RANDOM.nextFloat() * 0.05f;
            this.colours[i] = COLOURS[RANDOM.nextInt(COLOURS.length)];
        }
    }

    @Override
    protected int lifetime() {
        return LIFE;
    }

    @Override
    public void lit(FxDraw draw, float t) {
        for (int i = 0; i < this.velocities.length; i++) {
            Vec3 drop = flight(this.at, this.velocities[i], t, 0.04f, 0.045f);
            if (drop.y < this.surface) {
                continue;
            }
            float half = this.halves[i];
            draw.box(drop, new Vector3f(half, half, half), UPRIGHT, this.colours[i], 1f);
        }
    }
}
