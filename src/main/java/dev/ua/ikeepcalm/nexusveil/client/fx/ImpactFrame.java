package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3fc;

import java.util.Random;

// A few hand-timed frames on a 24 fps clock: an inverted frame, an ink plate, then strokes falling away over the world.
public class ImpactFrame {
    private static final long FRAME_MS = 42;
    private static final long COOLDOWN_MS = 450;
    private static final int TAIL_FRAMES = 5;
    private static final int INK = 0xFF0A0A0A;
    private static final int CHALK = 0xFFF6F3EC;
    private static final int SPIKES = 22;

    private static Vec3 anchor;
    private static float intensity;
    private static int accent;
    private static long startedAt = -COOLDOWN_MS;
    private static final float[] ANGLES = new float[SPIKES];
    private static final float[] LENGTHS = new float[SPIKES];
    private static final float[] WIDTHS = new float[SPIKES];

    private ImpactFrame() {
    }

    // A broadside ripples a gun every few ticks; one frame per volley reads as a hit, one per gun as a strobe.
    public static void trigger(Vec3 at, float strength, int accentRgb) {
        long now = System.currentTimeMillis();
        if (now - startedAt < COOLDOWN_MS) {
            return;
        }
        startedAt = now;
        anchor = at;
        intensity = Math.clamp(strength, 0f, 1f);
        accent = 0xFF000000 | accentRgb;
        Random random = new Random(now);
        for (int i = 0; i < SPIKES; i++) {
            ANGLES[i] = (float) (Math.PI * 2 * (i + random.nextFloat() * 0.6f) / SPIKES);
            LENGTHS[i] = 0.35f + random.nextFloat() * 0.65f;
            WIDTHS[i] = 0.6f + random.nextFloat() * 0.8f;
        }
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker ignored) {
        if (anchor == null) {
            return;
        }
        int frame = (int) ((System.currentTimeMillis() - startedAt) / FRAME_MS);
        int plates = intensity > 0.7f ? 2 : 1;
        if (frame >= 1 + plates + TAIL_FRAMES) {
            anchor = null;
            return;
        }
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        float[] focus = focus(anchor, w, h);
        float reach = Math.max(w, h) * (0.45f + 0.35f * intensity);
        if (frame == 0) {
            ctx.fill(RenderPipelines.GUI_INVERT, 0, 0, w, h, 0xFFFFFFFF);
            spikes(ctx, focus, reach, 1f, 1f, INK);
            return;
        }
        if (frame <= plates) {
            ctx.fill(0, 0, w, h, (Math.round(200 * intensity + 40) << 24) | (INK & 0xFFFFFF));
            spikes(ctx, focus, reach * 1.05f, 1.6f, 1f, accent);
            spikes(ctx, focus, reach, 1f, 1f, CHALK);
            return;
        }
        float q = (frame - plates) / (float) (TAIL_FRAMES + 1);
        float keep = (1f - q) * (1f - q);
        spikes(ctx, focus, reach * keep, 1.5f * keep, q, CHALK);
        spikes(ctx, focus, reach * keep, keep, q, INK);
    }

    private static void spikes(GuiGraphicsExtractor ctx, float[] focus, float reach, float widthScale, float lift, int colour) {
        Matrix3x2fStack pose = ctx.pose();
        float unit = Math.max(1f, reach / 120f);
        for (int i = 0; i < SPIKES; i++) {
            float length = reach * LENGTHS[i];
            float base = reach * 0.08f + reach * 0.5f * lift * LENGTHS[i];
            float half = unit * 3.5f * WIDTHS[i] * widthScale;
            pose.pushMatrix();
            pose.translate(focus[0], focus[1]);
            pose.rotate(ANGLES[i]);
            // a taper out of stacked bars, since a GUI fill is only ever a rectangle
            for (int step = 0; step < 4; step++) {
                float from = base + length * step / 4f;
                float to = base + length * (step + 1) / 4f;
                int thickness = Math.max(1, Math.round(half * (1f - step / 4f)));
                ctx.fill(Math.round(from), -thickness, Math.round(to), thickness, colour);
            }
            pose.popMatrix();
        }
    }

    private static float[] focus(Vec3 at, int w, int h) {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized() || camera.getFov() <= 0f) {
            return new float[]{w / 2f, h / 2f};
        }
        Vec3 rel = at.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        double depth = rel.x * forward.x() + rel.y * forward.y() + rel.z * forward.z();
        if (depth <= 0.1) {
            return new float[]{w / 2f, h * 0.75f};
        }
        double side = -(rel.x * left.x() + rel.y * left.y() + rel.z * left.z());
        double rise = rel.x * up.x() + rel.y * up.y() + rel.z * up.z();
        double tanY = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double tanX = tanY * w / h;
        float x = (float) ((side / depth / tanX + 1) * 0.5 * w);
        float y = (float) ((1 - rise / depth / tanY) * 0.5 * h);
        return new float[]{Math.clamp(x, 0f, w), Math.clamp(y, 0f, h)};
    }
}
