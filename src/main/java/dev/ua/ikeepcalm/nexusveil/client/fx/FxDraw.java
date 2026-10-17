package dev.ua.ikeepcalm.nexusveil.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// Untextured translucent primitives in world coordinates; soft edges are per-vertex alpha, never a texture.
public class FxDraw {
    private static final int SEGMENTS = 16;

    private final PoseStack.Pose pose;
    private final VertexConsumer consumer;
    private final Vec3 camera;
    private final Vector3f right;
    private final Vector3f up;
    private int light;

    public FxDraw(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera, Vector3fc left, Vector3fc up, int light) {
        this.pose = pose;
        this.consumer = consumer;
        this.camera = camera;
        this.right = new Vector3f(left).negate();
        this.up = new Vector3f(up);
        this.light = light;
    }

    public void light(int light) {
        this.light = light;
    }

    public Vec3 camera() {
        return this.camera;
    }

    public void billboardRing(Vec3 centre, float radius, float thickness, int rgb, float alpha) {
        float in = Math.max(0.01f, radius - thickness);
        float out = radius + thickness;
        for (int i = 0; i < SEGMENTS * 2; i++) {
            double a1 = Math.PI * i / SEGMENTS;
            double a2 = Math.PI * (i + 1) / SEGMENTS;
            billboard(centre, Math.cos(a1) * in, Math.sin(a1) * in, rgb, 0f);
            billboard(centre, Math.cos(a2) * in, Math.sin(a2) * in, rgb, 0f);
            billboard(centre, Math.cos(a2) * radius, Math.sin(a2) * radius, rgb, alpha);
            billboard(centre, Math.cos(a1) * radius, Math.sin(a1) * radius, rgb, alpha);
            billboard(centre, Math.cos(a1) * radius, Math.sin(a1) * radius, rgb, alpha);
            billboard(centre, Math.cos(a2) * radius, Math.sin(a2) * radius, rgb, alpha);
            billboard(centre, Math.cos(a2) * out, Math.sin(a2) * out, rgb, 0f);
            billboard(centre, Math.cos(a1) * out, Math.sin(a1) * out, rgb, 0f);
        }
    }

    // One textured quad lying on the water, turned by angle so neighbouring patches never repeat.
    public void sprite(Vec3 centre, float radius, float angle, int rgb, float alpha) {
        float cos = (float) Math.cos(angle) * radius;
        float sin = (float) Math.sin(angle) * radius;
        quad(centre.add(-cos + sin, 0, -sin - cos), centre.add(-cos - sin, 0, -sin + cos),
                centre.add(cos - sin, 0, sin + cos), centre.add(cos + sin, 0, sin - cos), 0f, 0f, 1f, 1f, rgb, alpha);
    }

    // A camera-facing textured quad cut from a sheet: (u0,v0) is the sprite's top-left, (u1,v1) its bottom-right.
    public void billboardSprite(Vec3 centre, float halfWidth, float halfHeight, float angle,
                                float u0, float v0, float u1, float v1, int rgb, float alpha) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        Vec3 x = new Vec3(this.right.x * cos + this.up.x * sin, this.right.y * cos + this.up.y * sin, this.right.z * cos + this.up.z * sin)
                .scale(halfWidth);
        Vec3 y = new Vec3(this.up.x * cos - this.right.x * sin, this.up.y * cos - this.right.y * sin, this.up.z * cos - this.right.z * sin)
                .scale(halfHeight);
        quad(centre.subtract(x).add(y), centre.subtract(x).subtract(y), centre.add(x).subtract(y), centre.add(x).add(y),
                u0, v0, u1, v1, rgb, alpha);
    }

    // Any textured quad, drawn from both sides: a gets (u0,v0), b (u0,v1), c (u1,v1), d (u1,v0).
    public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float v0, float u1, float v1, int rgb, float alpha) {
        uv(a, u0, v0, rgb, alpha);
        uv(b, u0, v1, rgb, alpha);
        uv(c, u1, v1, rgb, alpha);
        uv(d, u1, v0, rgb, alpha);
    }

    public Vector3f right() {
        return new Vector3f(this.right);
    }

    public Vector3f up() {
        return new Vector3f(this.up);
    }

    private void uv(Vec3 at, float u, float v, int rgb, float alpha) {
        this.consumer.addVertex(this.pose, (float) (at.x - this.camera.x), (float) (at.y - this.camera.y),
                        (float) (at.z - this.camera.z))
                .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (255 * Math.clamp(alpha, 0f, 1f)))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(this.light)
                .setNormal(this.pose, 0f, 1f, 0f);
    }

    // Streak from tail to head, always showing its width to the camera.
    public void streak(Vec3 head, Vec3 tail, float width, int rgb, float alpha) {
        Vec3 along = head.subtract(tail);
        Vec3 side = along.cross(head.subtract(this.camera));
        double length = side.length();
        if (length < 1.0e-6) {
            return;
        }
        side = side.scale(width / length);
        vertex(head.subtract(side), rgb, alpha);
        vertex(head.add(side), rgb, alpha);
        vertex(tail.add(side), rgb, alpha * 0.1f);
        vertex(tail.subtract(side), rgb, alpha * 0.1f);
    }

    public void box(Vec3 centre, Vector3f half, Quaternionf rotation, int rgb, float alpha) {
        Vector3f[] c = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            c[i] = rotation.transform(new Vector3f((i & 1) == 0 ? -half.x : half.x, (i & 2) == 0 ? -half.y : half.y,
                    (i & 4) == 0 ? -half.z : half.z));
        }
        face(centre, c[2], c[3], c[7], c[6], shade(rgb, 1.0f), alpha);
        face(centre, c[0], c[4], c[5], c[1], shade(rgb, 0.5f), alpha);
        face(centre, c[0], c[2], c[6], c[4], shade(rgb, 0.7f), alpha);
        face(centre, c[1], c[5], c[7], c[3], shade(rgb, 0.7f), alpha);
        face(centre, c[0], c[1], c[3], c[2], shade(rgb, 0.85f), alpha);
        face(centre, c[4], c[6], c[7], c[5], shade(rgb, 0.85f), alpha);
    }

    private void face(Vec3 centre, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int rgb, float alpha) {
        vertex(centre, a.x, a.y, a.z, rgb, alpha);
        vertex(centre, b.x, b.y, b.z, rgb, alpha);
        vertex(centre, c.x, c.y, c.z, rgb, alpha);
        vertex(centre, d.x, d.y, d.z, rgb, alpha);
    }

    private static int shade(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private void billboard(Vec3 centre, double x, double y, int rgb, float alpha) {
        vertex(centre, (float) (this.right.x * x + this.up.x * y), (float) (this.right.y * x + this.up.y * y),
                (float) (this.right.z * x + this.up.z * y), rgb, alpha);
    }

    private void vertex(Vec3 at, int rgb, float alpha) {
        vertex(at, 0f, 0f, 0f, rgb, alpha);
    }

    private void vertex(Vec3 at, float dx, float dy, float dz, int rgb, float alpha) {
        this.consumer.addVertex(this.pose, (float) (at.x - this.camera.x) + dx, (float) (at.y - this.camera.y) + dy,
                        (float) (at.z - this.camera.z) + dz)
                .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (255 * Math.clamp(alpha, 0f, 1f)))
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(this.light)
                .setNormal(this.pose, 0f, 1f, 0f);
    }
}
