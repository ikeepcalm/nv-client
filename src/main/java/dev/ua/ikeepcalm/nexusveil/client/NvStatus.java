package dev.ua.ikeepcalm.nexusveil.client;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCarry;
import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision.Site;
import dev.ua.ikeepcalm.nexusveil.client.net.NvNetworking;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipDrawPass;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.net.NvProtocol;
import dev.ua.ikeepcalm.nexusveil.core.net.NvWelcome;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NvStatus {

    private NvStatus() {
    }

    public static void print(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("NexusVeil Client, protocol " + NvProtocol.VERSION));

        line(source, "hello", NvNetworking.helloSent() ? "sent" : "NOT SENT (channel unavailable)");
        line(source, "payloads in", NvNetworking.payloadsIn() + " ("
                + bytes(NvNetworking.bytesIn()) + ")");

        NvWelcome welcome = NvNetworking.welcome();
        if (welcome == null) {
            source.sendFeedback(Component.literal(NvNetworking.helloSent()
                    ? "  server: said hello, no welcome -- transport fault, check the client log"
                    : "  server: not a NexusVeil server"));
            return;
        }
        line(source, "server", "plugin " + welcome.getPluginVersion() + ", protocol " + welcome.getProtocol());
        line(source, "renderer", ShipRenderer.available()
                ? "drawing %d pieces in %d batches".formatted(
                        ShipDrawPass.piecesLastFrame(), ShipDrawPass.batchesLastFrame())
                : "STOOD DOWN (hulls stay the server's)");
        line(source, "ships", ClientFleet.size() + " tracked, " + ClientFleet.drawable() + " with geometry");
        line(source, "geometry", ClientFleet.assembler().hullsFromCache() + " from cache, "
                + bytes(ClientFleet.assembler().downloadedBytes()) + " downloaded, "
                + ClientFleet.assembler().outstanding() + " still arriving");
        line(source, "traffic", bytes(NvNetworking.bytesIn()) + " in, "
                + bytes(NvNetworking.bytesOut()) + " out");
        line(source, "hulls", ShipRenderer.builtHulls() + " built, " + ShipRenderer.sections()
                + " sections, " + ShipRenderer.quads() + " quads, " + bytes(ShipRenderer.gpuBytes()) + " vram, "
                + "%.1f ms on thread + %.1f ms off".formatted(
                        ShipRenderer.meanPrepareMillis(), ShipRenderer.meanCompileMillis())
                + (ShipRenderer.pending() == 0 ? "" : ", " + ShipRenderer.pending() + " building")
                + (ShipRenderer.failures() == 0 ? "" : ", " + ShipRenderer.failures() + " FAILED"));
        line(source, "last move", ClientFleet.lastMoveTick() + " (" + ClientFleet.movesReceived() + " received)");

        for (ClientShip ship : ClientFleet.all()) {
            line(source, "  #" + ship.handle(), "%s  %s  yaw %.1f  heel %.1f  at %.1f %.1f %.1f".formatted(
                    ship.identity().getName(),
                    ship.drawable() ? "geometry ok" : "no geometry",
                    Math.toDegrees(ship.transform().getYawRadians()), Math.toDegrees(ship.pose().getRollRadians()),
                    ship.transform().getOrigin().getX(), ship.transform().getOrigin().getY(),
                    ship.transform().getOrigin().getZ()));
            String failure = ShipRenderer.failure(ship.handle());
            if (failure != null) {
                line(source, "     mesh", "FAILED " + failure);
            }
        }
        line(source, "collision", "move %d calls/%d shapes, query %d calls/%d shapes".formatted(
                ShipCollision.calls(Site.MOVEMENT),
                ShipCollision.shapes(Site.MOVEMENT),
                ShipCollision.calls(Site.QUERY),
                ShipCollision.shapes(Site.QUERY))
                + ", lithium %d calls/%d shapes".formatted(
                ShipCollision.calls(Site.LITHIUM),
                ShipCollision.shapes(Site.LITHIUM)));
        line(source, "carry", ShipCarry.carries()
                + " ticks carried");
        line(source, "fps", String.valueOf(Minecraft.getInstance().getFps()));
    }

    private static void line(FabricClientCommandSource source, String key, String value) {
        source.sendFeedback(Component.literal("  " + key + ": " + value));
    }

    private static String bytes(long value) {
        if (value < 1024) {
            return value + "B";
        }
        return value < 1024 * 1024 ? "%.1fKiB".formatted(value / 1024.0) : "%.1fMiB".formatted(value / 1048576.0);
    }
}
