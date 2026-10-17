package dev.ua.ikeepcalm.nexusveil.client.net;

import dev.ua.ikeepcalm.nexusveil.core.net.NvProtocol;
import lombok.Data;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class NvPayloads {

    private NvPayloads() {
    }

    @Data
    public static class Raw implements CustomPacketPayload {
        private final CustomPacketPayload.Type<Raw> id;
        private final byte[] data;

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return this.id;
        }
    }

    public static CustomPacketPayload.Type<Raw> type(String channel) {
        int colon = channel.indexOf(':');
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                channel.substring(0, colon), channel.substring(colon + 1)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, Raw> codec(CustomPacketPayload.Type<Raw> type, int cap) {
        return StreamCodec.ofMember(
                (value, buf) -> buf.writeBytes(value.getData()),
                buf -> {
                    int length = buf.readableBytes();
                    if (length > cap) {
                        throw new IllegalArgumentException(
                                "NexusVeil payload of " + length + " bytes exceeds the " + cap + " cap");
                    }
                    byte[] data = new byte[length];
                    buf.readBytes(data);
                    return new Raw(type, data);
                });
    }

    public static final CustomPacketPayload.Type<Raw> HELLO = type(NvProtocol.HELLO);
    public static final CustomPacketPayload.Type<Raw> GEOMETRY_REQUEST = type(NvProtocol.GEOMETRY_REQUEST);
    public static final CustomPacketPayload.Type<Raw> ABOARD = type(NvProtocol.ABOARD);
    public static final CustomPacketPayload.Type<Raw> LEAVE = type(NvProtocol.LEAVE);
    public static final CustomPacketPayload.Type<Raw> READY = type(NvProtocol.READY);

    public static final CustomPacketPayload.Type<Raw> WELCOME = type(NvProtocol.WELCOME);
    public static final CustomPacketPayload.Type<Raw> SHIP_ADD = type(NvProtocol.SHIP_ADD);
    public static final CustomPacketPayload.Type<Raw> GEOMETRY = type(NvProtocol.GEOMETRY);
    public static final CustomPacketPayload.Type<Raw> SHIP_MOVE = type(NvProtocol.SHIP_MOVE);
    public static final CustomPacketPayload.Type<Raw> SHIP_EDIT = type(NvProtocol.SHIP_EDIT);
    public static final CustomPacketPayload.Type<Raw> SHIP_REMOVE = type(NvProtocol.SHIP_REMOVE);
    public static final CustomPacketPayload.Type<Raw> CORRECT = type(NvProtocol.CORRECT);
    public static final CustomPacketPayload.Type<Raw> EFFECT = type(NvProtocol.EFFECT);
}
