package dev.ua.ikeepcalm.nexusveil.client.fx;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Vfx {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("nexusveil-client", "textures/fx/white.png");
    private static final int CAP = 2500;
    private static final double REACH = 192.0;

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final List<Fx> SPAWNED = new ArrayList<>();

    private Vfx() {
    }

    public static void add(Fx fx) {
        SPAWNED.add(fx);
    }

    public static boolean crowded() {
        return ACTIVE.size() + SPAWNED.size() >= CAP;
    }

    public static int size() {
        return ACTIVE.size();
    }

    public static void tick() {
        ACTIVE.removeIf(Fx::tick);
        ACTIVE.addAll(SPAWNED);
        SPAWNED.clear();
    }

    public static void clear() {
        ACTIVE.clear();
        SPAWNED.clear();
    }

    public static void draw(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || minecraft.level == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = context.levelState().cameraRenderState.pos;
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<Fx> visible = new ArrayList<>();
        for (Fx fx : ACTIVE) {
            if (fx.at().distanceToSqr(eye) < REACH * REACH) {
                visible.add(fx);
            }
        }
        context.submitNodeCollector().order(900).submitCustomGeometry(context.poseStack(),
                RenderTypes.entityTranslucent(WHITE), (pose, consumer) -> {
                    FxDraw draw = new FxDraw(pose, consumer, eye, camera.leftVector(), camera.upVector(), 0);
                    for (Fx fx : visible) {
                        draw.light(LightCoordsUtil.getLightCoords(minecraft.level, BlockPos.containing(fx.at().add(0, 0.5, 0))));
                        fx.lit(draw, fx.age + partial);
                    }
                });
        Map<Identifier, List<Fx>> sheets = new LinkedHashMap<>();
        for (Fx fx : visible) {
            if (fx.sheet() != null) {
                sheets.computeIfAbsent(fx.sheet(), ignored -> new ArrayList<>()).add(fx);
            }
        }
        sheets.forEach((sheet, users) -> {
            List<Fx> lit = users.stream().filter(fx -> !fx.sheetGlows()).toList();
            List<Fx> glowing = users.stream().filter(Fx::sheetGlows).toList();
            if (!lit.isEmpty()) {
                context.submitNodeCollector().order(899).submitCustomGeometry(context.poseStack(),
                        RenderTypes.entityTranslucent(sheet), (pose, consumer) -> {
                            FxDraw draw = new FxDraw(pose, consumer, eye, camera.leftVector(), camera.upVector(), 0);
                            for (Fx fx : lit) {
                                draw.light(LightCoordsUtil.getLightCoords(minecraft.level, BlockPos.containing(fx.at().add(0, 0.5, 0))));
                                fx.sheet(draw, fx.age + partial);
                            }
                        });
            }
            if (!glowing.isEmpty()) {
                context.submitNodeCollector().order(902).submitCustomGeometry(context.poseStack(),
                        RenderTypes.entityTranslucentEmissive(sheet), (pose, consumer) -> {
                            FxDraw draw = new FxDraw(pose, consumer, eye, camera.leftVector(), camera.upVector(),
                                    LightCoordsUtil.FULL_BRIGHT);
                            for (Fx fx : glowing) {
                                fx.sheet(draw, fx.age + partial);
                            }
                        });
            }
        });
        context.submitNodeCollector().order(901).submitCustomGeometry(context.poseStack(),
                RenderTypes.entityTranslucentEmissive(WHITE), (pose, consumer) -> {
                    FxDraw draw = new FxDraw(pose, consumer, eye, camera.leftVector(), camera.upVector(),
                            LightCoordsUtil.FULL_BRIGHT);
                    for (Fx fx : visible) {
                        fx.glow(draw, fx.age + partial);
                    }
                });
    }
}
