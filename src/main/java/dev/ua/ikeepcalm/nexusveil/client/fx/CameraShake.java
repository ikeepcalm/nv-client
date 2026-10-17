package dev.ua.ikeepcalm.nexusveil.client.fx;

// View only: the player entity is never moved, so no movement check can trip on it.
public class CameraShake {
    private static float strength;
    private static long startedAt;
    private static long lasting;

    private CameraShake() {
    }

    public static void kick(float degrees, long millis) {
        long now = System.currentTimeMillis();
        if (degrees > current(now)) {
            strength = degrees;
            startedAt = now;
            lasting = millis;
        }
    }

    public static float yaw() {
        long now = System.currentTimeMillis();
        return current(now) * (float) Math.sin((now - startedAt) * 0.075);
    }

    public static float pitch() {
        long now = System.currentTimeMillis();
        return current(now) * 0.7f * (float) Math.sin((now - startedAt) * 0.11 + 1.3);
    }

    private static float current(long now) {
        float p = (now - startedAt) / (float) Math.max(1, lasting);
        return p >= 1f ? 0f : strength * (1f - p) * (1f - p);
    }
}
