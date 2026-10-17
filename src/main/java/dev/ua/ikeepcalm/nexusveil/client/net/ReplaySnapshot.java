package dev.ua.ikeepcalm.nexusveil.client.net;

import dev.ua.ikeepcalm.nexusveil.client.ship.ClientFleet;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipMove;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// A Flashback recording starts mid-session, after the handshake and every ship_add it needs; this replays them into
// each snapshot so playback rebuilds the fleet. Geometry is not written: the replaying client reads its own cache.
public class ReplaySnapshot {

    private ReplaySnapshot() {
    }

    public static void write(Consumer<Packet<? super ClientGamePacketListener>> out) {
        if (!NvNetworking.connected()) {
            return;
        }
        out.accept(packet(NvPayloads.WELCOME, NvNetworking.welcome().encode()));
        List<NvShipMove.Entry> poses = new ArrayList<>();
        long tick = 0;
        for (ClientShip ship : ClientFleet.all()) {
            out.accept(packet(NvPayloads.SHIP_ADD, ship.identity().encode()));
            if (ship.lastMove() != null) {
                poses.add(ship.lastMove());
                tick = Math.max(tick, ship.tick());
            }
        }
        if (!poses.isEmpty()) {
            out.accept(packet(NvPayloads.SHIP_MOVE, new NvShipMove(tick, poses).encode()));
        }
    }

    private static ClientboundCustomPayloadPacket packet(CustomPacketPayload.Type<NvPayloads.Raw> type, byte[] data) {
        return new ClientboundCustomPayloadPacket(new NvPayloads.Raw(type, data));
    }
}
