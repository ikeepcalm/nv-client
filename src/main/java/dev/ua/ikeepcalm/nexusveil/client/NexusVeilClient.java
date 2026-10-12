package dev.ua.ikeepcalm.nexusveil.client;

import dev.ua.ikeepcalm.nexusveil.client.config.NvSettings;
import dev.ua.ikeepcalm.nexusveil.client.net.NvNetworking;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipDrawPass;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipPick;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.net.NvProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.network.chat.Component;

public class NexusVeilClient implements ClientModInitializer {

    public static final String MOD_ID = "nexusveil-client";

    @Override
    public void onInitializeClient() {
        NvSettings.load();
        NvNetworking.register();
        // Before the player's own tick: the hull moves the frame they stand in, and then they
        // walk in it. The other way round and every step is resolved against last tick's deck.
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientFleet.latch());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(context -> ShipDrawPass.draw(
                context.levelState().cameraRenderState, ChunkSectionLayerGroup.OPAQUE));
        LevelRenderEvents.COLLECT_SUBMITS.register(ShipPick::draw);
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
        for (ClientShip ship : ClientFleet.all()) {
            ShipRenderer.mesh(ship);
            // Gated on the renderer, not the mesh: an undrawable hull must not make the server stop drawing it.
            if (ShipRenderer.available() && ship.takeAnnouncement()) {
                NvNetworking.ready(ship.handle(), true);
            }
        }
    }
}
