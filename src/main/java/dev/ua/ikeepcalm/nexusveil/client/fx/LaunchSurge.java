package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// A hull coming up out of the sea: water sheets off her sides and rings run out along her whole length.
public class LaunchSurge extends Fx {
    private final Vec3 forward;
    private final Vec3 starboard;
    private final float halfLength;
    private final float halfBeam;

    public LaunchSurge(Vec3 centre, Vec3 keel, float halfBeam) {
        super(centre);
        this.halfLength = (float) keel.length();
        this.forward = keel.normalize();
        this.starboard = new Vec3(-this.forward.z, 0, this.forward.x);
        this.halfBeam = halfBeam;
    }

    @Override
    protected int lifetime() {
        return 80;
    }

    @Override
    public boolean tick() {
        if (this.age < 36 && !Vfx.crowded()) {
            int bursts = this.age < 12 ? 4 : 2;
            for (int i = 0; i < bursts; i++) {
                Vfx.add(new WaterSplash(rim(RANDOM.nextDouble() * Math.PI * 2, 0.6),
                        0.5f + RANDOM.nextFloat() * 0.6f * (1f - this.age / 36f)));
            }
            for (int i = 0; i < 6; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                Vec3 out = rim(angle, 1.0).subtract(this.at).normalize();
                Vfx.add(new Foam(rim(angle, 0.3).add(0, 0.02, 0), out.scale(0.05), 0.8f, 2.6f, 90 + RANDOM.nextInt(40), 0.7f));
            }
        }
        return super.tick();
    }

    @Override
    public Identifier sheet() {
        return SplashSheet.TEXTURE;
    }

    @Override
    public void sheet(FxDraw draw, float t) {
        for (int i = 0; i < 4; i++) {
            float p = (t - i * 8f) / 60f;
            if (p <= 0f || p >= 1f) {
                continue;
            }
            float grow = 1.0f + 10f * easeOut(p);
            SplashSheet.ripple(draw, this.at.add(0, 0.03, 0), this.forward, this.starboard, this.halfLength + grow,
                    this.halfBeam + grow, SplashSheet.rippleFrame(p), 0.85f * (float) Math.pow(1f - p, 1.4));
        }
    }

    private Vec3 rim(double angle, double margin) {
        return this.at.add(this.forward.scale(Math.cos(angle) * (this.halfLength + margin)))
                .add(this.starboard.scale(Math.sin(angle) * (this.halfBeam + margin)));
    }
}
