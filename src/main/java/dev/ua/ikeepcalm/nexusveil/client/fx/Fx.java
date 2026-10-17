package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

// Animated on game ticks plus the partial tick, never the wall clock, so a Flashback export renders it at its own pace.
public abstract class Fx {
    protected static final RandomSource RANDOM = RandomSource.create();

    protected final Vec3 at;
    protected int age;

    protected Fx(Vec3 at) {
        this.at = at;
    }

    public Vec3 at() {
        return this.at;
    }

    protected abstract int lifetime();

    public boolean tick() {
        return ++this.age >= lifetime();
    }

    public void glow(FxDraw draw, float t) {
    }

    public void lit(FxDraw draw, float t) {
    }

    // The texture sheet() draws with; null means this effect draws no textured geometry.
    public Identifier sheet() {
        return null;
    }

    // Full-bright instead of lit by the world, for fire and flashes drawn from a texture.
    public boolean sheetGlows() {
        return false;
    }

    public void sheet(FxDraw draw, float t) {
    }

    protected static float fade(float t, float start, float end) {
        return 1f - Math.clamp((t - start) / (end - start), 0f, 1f);
    }

    protected static float easeOut(float p) {
        float q = 1f - Math.clamp(p, 0f, 1f);
        return 1f - q * q * q;
    }

    protected static Vec3 spread(double amount) {
        return new Vec3((RANDOM.nextDouble() * 2 - 1) * amount, (RANDOM.nextDouble() * 2 - 1) * amount,
                (RANDOM.nextDouble() * 2 - 1) * amount);
    }

    // Closed form for a drag-and-gravity flight, so it is smooth between ticks at any framerate.
    protected static Vec3 flight(Vec3 from, Vec3 velocity, float t, float drag, float gravity) {
        double travel = drag <= 0f ? t : (1.0 - Math.exp(-drag * t)) / drag;
        return from.add(velocity.scale(travel)).add(0.0, -0.5 * gravity * t * t, 0.0);
    }
}
