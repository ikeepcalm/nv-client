package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A lump of white water as a cluster of axis-aligned cubes that bob, drift and melt back into the sea.
public class WakeCrest extends Fx {
    private static final Quaternionf UPRIGHT = new Quaternionf();
    private static final int[] COLOURS = {0xFFFFFF, 0xFFFFFF, 0xE6F2FC, 0xBFDDF5};

    private final Vec3 drift;
    private final int life;
    private final Vec3[] offsets;
    private final float[] halves;
    private final int[] colours;
    private final float[] phases;

    public WakeCrest(Vec3 at, Vec3 drift, int cubes, float size, int life) {
        super(at);
        this.drift = drift;
        this.life = life;
        this.offsets = new Vec3[cubes];
        this.halves = new float[cubes];
        this.colours = new int[cubes];
        this.phases = new float[cubes];
        for (int i = 0; i < cubes; i++) {
            this.halves[i] = size * (0.5f + RANDOM.nextFloat() * 0.6f);
            this.offsets[i] = new Vec3((RANDOM.nextDouble() - 0.5) * size * 2.5, 0, (RANDOM.nextDouble() - 0.5) * size * 2.5);
            this.colours[i] = COLOURS[RANDOM.nextInt(COLOURS.length)];
            this.phases[i] = RANDOM.nextFloat() * (float) Math.PI * 2f;
        }
    }

    @Override
    protected int lifetime() {
        return this.life;
    }

    @Override
    public void lit(FxDraw draw, float t) {
        float p = t / this.life;
        float shrink = (1f - p * p) * Math.min(1f, t / 3f);
        Vec3 centre = flight(this.at, this.drift, t, 0.05f, 0f);
        for (int i = 0; i < this.offsets.length; i++) {
            float half = this.halves[i] * shrink;
            if (half < 0.02f) {
                continue;
            }
            double bob = 0.04 * Math.sin(t * 0.35 + this.phases[i]);
            draw.box(centre.add(this.offsets[i]).add(0, half * 0.6 + bob, 0), new Vector3f(half, half, half), UPRIGHT,
                    this.colours[i], fade(p, 0.75f, 1f));
        }
    }
}
