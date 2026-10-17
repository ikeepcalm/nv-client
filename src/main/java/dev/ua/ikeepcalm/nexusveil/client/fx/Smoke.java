package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// One translucent cube of smoke: thrown, then left to the wind, growing, tumbling and paling as it thins.
public class Smoke extends Fx {
    private static final Vec3 WIND = new Vec3(0.012, 0.0, 0.005);
    private static final float DRAG = 0.1f;

    private final Vec3 velocity;
    private final Vec3 lift;
    private final float from;
    private final float to;
    private final int life;
    private final int dark;
    private final int pale;
    private final float alpha;
    private final boolean hot;
    private final Vector3f axis = new Vector3f(RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f).normalize();
    private final float spin = (RANDOM.nextFloat() - 0.5f) * 0.03f;
    private final Quaternionf base = new Quaternionf().rotateXYZ(RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f);

    public Smoke(Vec3 at, Vec3 velocity, float rise, float from, float to, int life, int dark, int pale, float alpha, boolean hot) {
        super(at);
        this.velocity = velocity;
        this.lift = WIND.add(0, rise, 0).add(spread(0.007));
        this.from = from;
        this.to = to;
        this.life = life;
        this.dark = dark;
        this.pale = pale;
        this.alpha = alpha;
        this.hot = hot;
    }

    @Override
    protected int lifetime() {
        return this.life;
    }

    private Vec3 position(float t) {
        return flight(this.at, this.velocity, t, DRAG, 0f).add(this.lift.scale(t));
    }

    private float half(float t) {
        return this.from + (this.to - this.from) * (0.5f * easeOut(t / 15f) + 0.5f * easeOut(t / this.life));
    }

    private Quaternionf rotation(float t) {
        return new Quaternionf(this.base).rotateAxis(this.spin * t, this.axis);
    }

    @Override
    public void lit(FxDraw draw, float t) {
        float p = t / this.life;
        float a = this.alpha * Math.min(1f, t / 3f) * (float) Math.pow(1f - p, 1.4);
        float h = half(t);
        draw.box(position(t), new Vector3f(h, h, h), rotation(t), blend(this.dark, this.pale, easeOut(p * 2f)), a);
    }

    @Override
    public void glow(FxDraw draw, float t) {
        if (this.hot && t < 8f) {
            float h = half(t) * 0.9f;
            draw.box(position(t), new Vector3f(h, h, h), rotation(t), 0xFF7A20, 0.5f * fade(t, 0f, 8f));
        }
    }

    static int blend(int from, int to, float k) {
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * k);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * k);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return (r << 16) | (g << 8) | b;
    }
}
