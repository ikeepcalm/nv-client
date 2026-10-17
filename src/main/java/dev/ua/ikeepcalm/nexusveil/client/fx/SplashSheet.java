package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// The one atlas every splash draws from, written by tools/splash.py: column frames above, dashed ripple frames below.
public class SplashSheet {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("nexusveil-client", "textures/fx/splash_sheet.png");
    public static final int COLUMN_FRAMES = 10;
    public static final int RIPPLE_FRAMES = 4;
    private static final float FRAME_U = 1f / COLUMN_FRAMES;
    private static final float COLUMN_V = 48f / 80f;
    private static final float INSET = 0.5f / 320f;

    private SplashSheet() {
    }

    // Three crossed sheets so the column reads as a solid from every side; height is 1.5x the width like the frame.
    public static void column(FxDraw draw, Vec3 base, float yaw, float width, int frame, float alpha) {
        float u0 = frame * FRAME_U + INSET;
        float u1 = (frame + 1) * FRAME_U - INSET;
        Vec3 up = new Vec3(0, width * 1.5, 0);
        for (int i = 0; i < 3; i++) {
            double angle = yaw + Math.PI * i / 3;
            Vec3 across = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(width / 2);
            draw.quad(base.subtract(across).add(up), base.subtract(across), base.add(across), base.add(across).add(up),
                    u0, INSET, u1, COLUMN_V - INSET, 0xFFFFFF, alpha);
        }
    }

    // A dashed ring lying on the water, stretched along u and v so it can hug a hull.
    public static void ripple(FxDraw draw, Vec3 centre, Vec3 u, Vec3 v, float a, float b, int frame, float alpha) {
        float u0 = frame * FRAME_U + INSET;
        float u1 = (frame + 1) * FRAME_U - INSET;
        Vec3 x = u.scale(a);
        Vec3 z = v.scale(b);
        draw.quad(centre.subtract(x).subtract(z), centre.subtract(x).add(z), centre.add(x).add(z), centre.add(x).subtract(z),
                u0, COLUMN_V + INSET, u1, 1f - INSET, 0xFFFFFF, alpha);
    }

    public static int rippleFrame(float p) {
        return Math.min(RIPPLE_FRAMES - 1, (int) (p * RIPPLE_FRAMES));
    }
}
