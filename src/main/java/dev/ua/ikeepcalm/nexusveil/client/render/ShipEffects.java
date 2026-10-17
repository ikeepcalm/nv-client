package dev.ua.ikeepcalm.nexusveil.client.render;

import dev.ua.ikeepcalm.nexusveil.client.config.NvSettings;
import dev.ua.ikeepcalm.nexusveil.client.fx.CameraShake;
import dev.ua.ikeepcalm.nexusveil.client.fx.HullHit;
import dev.ua.ikeepcalm.nexusveil.client.fx.HullWake;
import dev.ua.ikeepcalm.nexusveil.client.fx.ImpactFrame;
import dev.ua.ikeepcalm.nexusveil.client.fx.LaunchSurge;
import dev.ua.ikeepcalm.nexusveil.client.fx.MuzzleBlast;
import dev.ua.ikeepcalm.nexusveil.client.fx.Vfx;
import dev.ua.ikeepcalm.nexusveil.client.fx.WaterSplash;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import dev.ua.ikeepcalm.nexusveil.core.net.NvEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// What the server sends is a moment; how it looks is decided here.
public class ShipEffects {

    private static final double FRAME_REACH = 28.0;
    private static final int POWDER = 0xFF7A22;

    private ShipEffects() {
    }

    public static void play(NvEffect effect) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!NvSettings.get().isEffects() || level == null) {
            return;
        }
        Vec3 at = vec(effect.getAt());
        Vec3 direction = unit(vec(effect.getDirection()));
        switch (effect.getKind()) {
            case CANNON -> {
                Vfx.add(new MuzzleBlast(at, direction));
                punch(at, 1.0f, 2.2f);
            }
            case HIT -> {
                Vfx.add(new HullHit(at, direction, blockOf(level, effect.getBlock()).getMapColor(level, BlockPos.ZERO).col, surface(level)));
                punch(at, 0.8f, 1.4f);
            }
            case SPLASH -> {
                Vec3 surface = new Vec3(at.x, surface(level), at.z);
                Vfx.add(new WaterSplash(surface, 1.0f));
                for (int i = 0; i < 16; i++) {
                    level.addParticle(ParticleTypes.BUBBLE, true, true, at.x + Math.random() - 0.5, surface.y - 0.3 - Math.random(),
                            at.z + Math.random() - 0.5, 0, 0.05, 0);
                }
            }
            case LAUNCH -> Vfx.add(new LaunchSurge(new Vec3(at.x, surface(level), at.z), vec(effect.getDirection()), effect.getSize()));
        }
    }

    public static void tick(Iterable<ClientShip> ships) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!NvSettings.get().isEffects() || level == null) {
            Vfx.clear();
            return;
        }
        Vfx.tick();
        double surface = surface(level);
        for (ClientShip ship : ships) {
            HullWake.tick(ship, surface);
        }
    }

    public static void forget(int handle) {
        HullWake.forget(handle);
    }

    public static void clear() {
        Vfx.clear();
        HullWake.clear();
    }

    private static void punch(Vec3 at, float strength, float shakeDegrees) {
        var viewer = Minecraft.getInstance().getCameraEntity();
        if (viewer == null) {
            return;
        }
        double distance = viewer.position().distanceTo(at);
        if (distance > FRAME_REACH) {
            return;
        }
        float near = (float) (1.0 - distance / FRAME_REACH);
        if (NvSettings.get().isImpactFrames()) {
            ImpactFrame.trigger(at, strength * (0.4f + 0.6f * near), POWDER);
        }
        if (NvSettings.get().isShake()) {
            CameraShake.kick(shakeDegrees * near, 380);
        }
    }

    // a full water block's top is 8/9 of the way up, so the visible sea sits just under sea level
    private static double surface(ClientLevel level) {
        return level.getSeaLevel() - 0.12;
    }

    private static BlockState blockOf(ClientLevel level, String key) {
        try {
            return BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK), key, false).blockState();
        } catch (Exception e) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
    }

    private static Vec3 vec(Vec3d v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    private static Vec3 unit(Vec3 v) {
        return v.lengthSqr() < 1.0e-12 ? new Vec3(0, 1, 0) : v.normalize();
    }
}
