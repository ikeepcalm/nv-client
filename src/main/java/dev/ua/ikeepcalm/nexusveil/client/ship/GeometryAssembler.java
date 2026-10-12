package dev.ua.ikeepcalm.nexusveil.client.ship;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.core.net.NvGeometry;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipAdd;
import dev.ua.ikeepcalm.nexusveil.core.voxel.GeometryCodec;
import dev.ua.ikeepcalm.nexusveil.core.voxel.VoxelModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryAssembler {

    private static class Pending {
        private final Map<NvGeometry.Kind, List<NvGeometry>> parts = new EnumMap<>(NvGeometry.Kind.class);
        private long bytes;

        private boolean complete(NvGeometry.Kind kind) {
            List<NvGeometry> held = this.parts.get(kind);
            return held != null && !held.isEmpty() && held.size() == held.getFirst().getCount();
        }
    }

    private final Map<Integer, Pending> pending = new HashMap<>();
    private final GeometryCache cache;

    private long downloaded;
    private long fromCache;

    public GeometryAssembler(GeometryCache cache) {
        this.cache = cache;
    }

    public VoxelModel cached(NvShipAdd identity) {
        if (!this.cache.has(identity.getDigest())) {
            return null;
        }
        byte[] palette = this.cache.read(identity.getDigest(), "palette");
        byte[] indices = this.cache.read(identity.getDigest(), "indices");
        if (palette == null || indices == null) {
            return null;
        }
        try {
            VoxelModel model = decode(identity, palette, indices);
            this.fromCache++;
            return model;
        } catch (RuntimeException e) {
            NvLog.LOG.warn("Cached hull {} would not decode, asking for it again: {}",
                    identity.getDigest(), e.getMessage());
            return null;
        }
    }

    public VoxelModel accept(NvShipAdd identity, NvGeometry part) {
        Pending held = this.pending.computeIfAbsent(part.getHandle(), handle -> new Pending());
        held.parts.computeIfAbsent(part.getKind(), kind -> new ArrayList<>()).add(part);
        held.bytes += part.getChunk().length;
        if (!held.complete(NvGeometry.Kind.PALETTE) || !held.complete(NvGeometry.Kind.INDICES)) {
            return null;
        }
        this.pending.remove(part.getHandle());

        byte[] palette = NvGeometry.join(ordered(held, NvGeometry.Kind.PALETTE));
        byte[] indices = NvGeometry.join(ordered(held, NvGeometry.Kind.INDICES));
        VoxelModel model = decode(identity, palette, indices);
        this.cache.write(identity.getDigest(), palette, indices);
        this.downloaded += held.bytes;
        NvLog.LOG.info("Built hull {} from {} bytes: {}x{}x{}, {} solid",
                identity.getName(), held.bytes, model.sizeX(), model.sizeY(), model.sizeZ(),
                model.solidCount());
        return model;
    }

    // The protocol does not promise chunk order, and an out-of-order palette decodes into plausible wrong blocks.
    private static List<NvGeometry> ordered(Pending held, NvGeometry.Kind kind) {
        List<NvGeometry> parts = new ArrayList<>(held.parts.get(kind));
        parts.sort(Comparator.comparingInt(NvGeometry::getIndex));
        return parts;
    }

    private static VoxelModel decode(NvShipAdd identity, byte[] palette, byte[] indices) {
        return GeometryCodec.decode(identity.getSizeX(), identity.getSizeY(), identity.getSizeZ(),
                palette, indices);
    }

    public void forget(int handle) {
        this.pending.remove(handle);
    }

    public void clear() {
        this.pending.clear();
    }

    public int outstanding() {
        return this.pending.size();
    }

    public long downloadedBytes() {
        return this.downloaded;
    }

    public long hullsFromCache() {
        return this.fromCache;
    }
}
