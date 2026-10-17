package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// A patch of white water left lying on the sea: hull wash, wake and the skirt of a splash.
public class Foam extends Fx {
    public static final int WHITE_WATER = 0xF4F9FF;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("nexusveil-client", "textures/fx/foam.png");

    private final Vec3 drift;
    private final float from;
    private final float to;
    private final int life;
    private final float alpha;
    private final float angle = RANDOM.nextFloat() * (float) Math.PI * 2f;
    private final float turn = (RANDOM.nextFloat() - 0.5f) * 0.01f;

    public Foam(Vec3 at, Vec3 drift, float from, float to, int life, float alpha) {
        super(at);
        this.drift = drift;
        this.from = from;
        this.to = to;
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
        float a = this.alpha * Math.min(1f, t / 3f) * (float) Math.pow(1f - p, 1.6);
        draw.sprite(centre, this.from + (this.to - this.from) * easeOut(p), this.angle + this.turn * t, WHITE_WATER, a);
    }
}
