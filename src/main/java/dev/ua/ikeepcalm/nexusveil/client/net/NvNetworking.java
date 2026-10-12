package dev.ua.ikeepcalm.nexusveil.client.net;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.client.config.NvSettings;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.core.net.NvGeometry;
import dev.ua.ikeepcalm.nexusveil.core.net.NvGeometryRequest;
import dev.ua.ikeepcalm.nexusveil.core.net.NvHello;
import dev.ua.ikeepcalm.nexusveil.core.net.NvLeave;
import dev.ua.ikeepcalm.nexusveil.core.net.NvProtocol;
import dev.ua.ikeepcalm.nexusveil.core.net.NvReady;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipAdd;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipMove;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipRemove;
import dev.ua.ikeepcalm.nexusveil.core.net.NvWelcome;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

public class NvNetworking {

    private static NvWelcome welcome;
    private static boolean helloSent;
    private static long payloadsIn;
    private static long bytesIn;
    private static long bytesOut;

    private NvNetworking() {
    }

    public static void register() {
        serverbound(NvPayloads.HELLO);
        serverbound(NvPayloads.GEOMETRY_REQUEST);
        serverbound(NvPayloads.ABOARD);
        serverbound(NvPayloads.LEAVE);
        serverbound(NvPayloads.READY);

        clientbound(NvPayloads.WELCOME);
        clientbound(NvPayloads.SHIP_ADD);
        clientbound(NvPayloads.GEOMETRY);
        clientbound(NvPayloads.SHIP_MOVE);
        clientbound(NvPayloads.SHIP_EDIT);
        clientbound(NvPayloads.SHIP_REMOVE);
        clientbound(NvPayloads.CORRECT);

        receive(NvPayloads.WELCOME, payload -> welcome(NvWelcome.decode(payload)));
        receive(NvPayloads.SHIP_ADD, payload -> ClientFleet.add(NvShipAdd.decode(payload)));
        receive(NvPayloads.SHIP_MOVE, payload -> ClientFleet.move(NvShipMove.decode(payload)));
        receive(NvPayloads.SHIP_REMOVE, payload -> ClientFleet.remove(NvShipRemove.decode(payload)));
        receive(NvPayloads.GEOMETRY, payload -> ClientFleet.geometry(NvGeometry.decode(payload)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> hello());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static void hello() {
        reset();
        if (!NvSettings.get().isEnabled()) {
            return;
        }
        String version = FabricLoader.getInstance().getModContainer("nexusveil-client")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        helloSent = send(NvPayloads.HELLO, new NvHello(NvProtocol.VERSION, version).encode());
    }

    private static void welcome(NvWelcome received) {
        welcome = received;
        NvLog.LOG.info("NexusVeil server: plugin {}, protocol {}",
                received.getPluginVersion(), received.getProtocol());
        if (received.getProtocol() != NvProtocol.VERSION) {
            welcome = null;
            tell("NexusVeil: this server speaks protocol " + received.getProtocol()
                    + " and this mod speaks " + NvProtocol.VERSION + ". Ships stay vanilla.");
        }
    }

    public static void requestGeometry(int handle, String cachedDigest) {
        send(NvPayloads.GEOMETRY_REQUEST, new NvGeometryRequest(handle, cachedDigest).encode());
    }

    public static void ready(int handle, boolean drawing) {
        send(NvPayloads.READY, new NvReady(handle, drawing).encode());
    }

    public static void stand(String reason) {
        if (welcome == null) {
            return;
        }
        send(NvPayloads.LEAVE, new NvLeave(reason).encode());
        welcome = null;
        ClientFleet.clear();
        NvLog.LOG.warn("Handed the ships back to the server: {}", reason);
    }

    public static boolean connected() {
        return welcome != null;
    }

    public static NvWelcome welcome() {
        return welcome;
    }

    public static long bytesIn() {
        return bytesIn;
    }

    public static long bytesOut() {
        return bytesOut;
    }

    private static void reset() {
        welcome = null;
        helloSent = false;
        payloadsIn = 0;
        bytesIn = 0;
        bytesOut = 0;
        ClientFleet.clear();
    }

    public static boolean helloSent() {
        return helloSent;
    }

    public static long payloadsIn() {
        return payloadsIn;
    }

    private static boolean send(CustomPacketPayload.Type<NvPayloads.Raw> type, byte[] data) {
        if (!ClientPlayNetworking.canSend(type)) {
            return false;
        }
        bytesOut += data.length;
        ClientPlayNetworking.send(new NvPayloads.Raw(type, data));
        return true;
    }

    private static void receive(CustomPacketPayload.Type<NvPayloads.Raw> type, Consumer<byte[]> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                context.client().execute(() -> {
                    payloadsIn++;
                    bytesIn += payload.getData().length;
                    try {
                        handler.accept(payload.getData());
                    } catch (RuntimeException e) {
                        NvLog.LOG.error("Failed to read {}", type.id(), e);
                        stand("could not read " + type.id());
                    }
                }));
    }

    private static void serverbound(CustomPacketPayload.Type<NvPayloads.Raw> type) {
        PayloadTypeRegistry.serverboundPlay().register(type, NvPayloads.codec(type, NvProtocol.MAX_SERVERBOUND));
    }

    // Plain register, never registerLarge: that is Fabric-to-Fabric chunking and the Bukkit sender cannot speak it.
    private static void clientbound(CustomPacketPayload.Type<NvPayloads.Raw> type) {
        PayloadTypeRegistry.clientboundPlay()
                .register(type, NvPayloads.codec(type, NvProtocol.MAX_CLIENTBOUND));
    }

    private static void tell(String message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
}
