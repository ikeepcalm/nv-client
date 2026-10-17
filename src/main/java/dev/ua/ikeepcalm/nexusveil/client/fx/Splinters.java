package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Planks torn out of the hull: they tumble, fall, and float where they land.
public class Splinters extends Fx {
    private static final int LIFE = 400;
    private static final double GRAVITY = 0.04;

    private final int wood;
    private final double sea;
    private final Piece[] pieces;

    private static class Piece {
        private Vec3 position;
        private Vec3 previous;
        private Vec3 velocity;
        private final Quaternionf rotation = new Quaternionf();
        private final Quaternionf lastRotation = new Quaternionf();
        private Vector3f spin;
        private Vector3f half;
        private boolean afloat;
    }

    public Splinters(Vec3 at, Vec3 outward, int wood, double sea) {
        super(at);
        this.wood = wood;
        this.sea = sea;
        this.pieces = new Piece[14];
        for (int i = 0; i < this.pieces.length; i++) {
            Piece piece = new Piece();
            piece.position = at.add(spread(0.3));
            piece.previous = piece.position;
            piece.velocity = outward.scale(0.25 + RANDOM.nextDouble() * 0.45).add(spread(0.22)).add(0, 0.18, 0);
            piece.half = new Vector3f(0.04f + RANDOM.nextFloat() * 0.08f, 0.025f + RANDOM.nextFloat() * 0.03f,
                    0.12f + RANDOM.nextFloat() * 0.22f);
            piece.spin = new Vector3f(RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f, RANDOM.nextFloat() - 0.5f).mul(0.9f);
            piece.rotation.rotateXYZ(RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f, RANDOM.nextFloat() * 6f);
            piece.lastRotation.set(piece.rotation);
            this.pieces[i] = piece;
        }
    }

    @Override
    protected int lifetime() {
        return LIFE;
    }

    @Override
    public boolean tick() {
        for (Piece piece : this.pieces) {
            piece.previous = piece.position;
            piece.lastRotation.set(piece.rotation);
            if (piece.afloat) {
                piece.velocity = piece.velocity.scale(0.92);
                double bob = Math.sin((this.age + piece.half.z * 40) * 0.15) * 0.03;
                piece.position = new Vec3(piece.position.x + piece.velocity.x, this.sea + bob, piece.position.z + piece.velocity.z);
                piece.spin.mul(0.9f);
            } else {
                piece.velocity = piece.velocity.add(0, -GRAVITY, 0).scale(0.98);
                piece.position = piece.position.add(piece.velocity);
                if (piece.position.y <= this.sea) {
                    piece.afloat = true;
                    piece.velocity = new Vec3(piece.velocity.x * 0.3, 0, piece.velocity.z * 0.3);
                    Vfx.add(new WaterSplash(new Vec3(piece.position.x, this.sea, piece.position.z), 0.25f));
                }
            }
            piece.rotation.rotateXYZ(piece.spin.x, piece.spin.y, piece.spin.z);
        }
        return super.tick();
    }

    @Override
    public void lit(FxDraw draw, float t) {
        float alpha = fade(t, LIFE - 40, LIFE);
        float partial = t - this.age;
        for (Piece piece : this.pieces) {
            Vec3 at = piece.previous.lerp(piece.position, partial);
            draw.box(at, piece.half, new Quaternionf(piece.lastRotation).slerp(piece.rotation, partial), this.wood, alpha);
        }
    }
}
