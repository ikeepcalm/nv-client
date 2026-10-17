package dev.ua.ikeepcalm.nexusveil.client;

import dev.ua.ikeepcalm.nexusveil.client.config.NvSettings;
import dev.ua.ikeepcalm.nexusveil.client.fx.ImpactFrame;
import dev.ua.ikeepcalm.nexusveil.client.fx.Vfx;
import dev.ua.ikeepcalm.nexusveil.client.net.NvNetworking;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipDrawPass;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipEffects;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipPick;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipShaderPass;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.net.NvProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.Identifier;

public class NexusVeilClient implements ClientModInitializer {

    public static final String MOD_ID = "nexusveil-client";

    @Override
    public void onInitializeClient() {
        NvSettings.load();
        NvNetworking.register();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "hulls"),
                (ResourceManagerReloadListener) manager -> ShipRenderer.invalidate());
        // Before the player's own tick: the hull moves the frame they stand in, and then they
        // walk in it. The other way round and every step is resolved against last tick's deck.
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientFleet.latch());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(context -> ShipDrawPass.draw(
                context.levelState().cameraRenderState, ChunkSectionLayerGroup.OPAQUE));
        LevelRenderEvents.COLLECT_SUBMITS.register(ShipPick::draw);
        LevelRenderEvents.COLLECT_SUBMITS.register(Vfx::draw);
        LevelRenderEvents.COLLECT_SUBMITS.register(ShipShaderPass::draw);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "impact_frame"), ImpactFrame::render);
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(context -> ShipDrawPass.draw(
                context.levelState().cameraRenderState, ChunkSectionLayerGroup.TRANSLUCENT));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(ClientCommands.literal("nvclient")
                        .executes(command -> {
                            NvStatus.print(command.getSource());
                            return 1;
                        })
                        .then(ClientCommands.literal("off").executes(command -> {
                            NvNetworking.stand("turned off by the player");
                            command.getSource().sendFeedback(Component.literal(
                                    "NexusVeil: ships handed back to the server for this session."));
                            return 1;
                        }))));
        NvLog.LOG.info("NexusVeil Client ready, protocol {}{}", NvProtocol.VERSION,
                ShipRenderer.available() ? "" : " (renderer unavailable: hulls stay the server's)");
    }

    private static void tick() {
        if (!NvNetworking.connected()) {
            return;
        }
        ShipRenderer.collect();
        ShipEffects.tick(ClientFleet.all());
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.mesh(ship);
            // Gated on the renderer, not the mesh: an undrawable hull must not make the server stop drawing it.
            if (ship.announce(ShipRenderer.available())) {
                NvNetworking.ready(ship.handle(), ship.announced());
            }
        }
    }
}
