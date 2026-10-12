package dev.ua.ikeepcalm.nexusveil.client.render;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.client.ship.ClientShip;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ShipRenderer {

    @Data
    public static class Built {
        private final ShipGpu gpu;
        private final int sections;
        private final int quads;
    }

    private static final ShipMesher MESHER = new ShipMesher();
    private static final Map<Integer, Built> HULLS = new HashMap<>();
    private static final Map<Integer, String> FAILURES = new HashMap<>();
    // One thread: the mesher owns a single SectionCompiler and SectionBufferBuilderPack, neither shareable.
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("nv-mesher").factory());
    private static final Map<Integer, Future<ShipMesh>> PENDING = new HashMap<>();

    private static volatile long prepareNanos;
    private static volatile long compileNanos;
    private static int meshed;

    public static void mesh(ClientShip ship) {
        if (HULLS.containsKey(ship.handle()) || PENDING.containsKey(ship.handle()) || !ship.drawable()) {
            return;
        }
        try {
            long started = System.nanoTime();
            ShipMesher.Job job = MESHER.prepare(ship.model(), ship.transform().getOrigin());
            prepareNanos += System.nanoTime() - started;
            if (job == null) {
                return;
            }
            PENDING.put(ship.handle(), WORKER.submit(() -> {
                long compiling = System.nanoTime();
                ShipMesh mesh = MESHER.compile(job);
                compileNanos += System.nanoTime() - compiling;
                return mesh;
            }));
        } catch (RuntimeException | LinkageError e) {
            fail(ship.handle(), ship.identity().getName(), e);
        }
    }

    public static void collect() {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.entrySet().removeIf(entry -> {
            if (!entry.getValue().isDone()) {
                return false;
            }
            try {
                ShipMesh mesh = entry.getValue().get();
                int sections = mesh.sectionCount();
                int quads = mesh.quads();
                HULLS.put(entry.getKey(), new Built(
                        ShipGpu.upload(mesh, "nexusveil/hull" + entry.getKey()), sections, quads));
                meshed++;
            } catch (Exception e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                fail(entry.getKey(), "#" + entry.getKey(), cause);
            }
            return true;
        });
    }

    private static void fail(int handle, String name, Throwable e) {
        NvLog.LOG.error("Could not build hull {}", name, e);
        // Stored as an empty hull so it is not retried every tick; FAILURES tells it apart from a genuinely empty mesh.
        HULLS.put(handle, new Built(new ShipGpu(), 0, 0));
        FAILURES.put(handle, e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : ": " + e.getMessage()));
    }

    public static Built built(int handle) {
        return HULLS.get(handle);
    }

    public static String failure(int handle) {
        return FAILURES.get(handle);
    }

    public static int failures() {
        return FAILURES.size();
    }

    public static void forget(int handle) {
        FAILURES.remove(handle);
        Future<ShipMesh> pending = PENDING.remove(handle);
        if (pending != null) {
            pending.cancel(false);
        }
        Built gone = HULLS.remove(handle);
        if (gone != null) {
            ShipGpu.closeQuietly(gone.getGpu());
        }
    }

    public static void clear() {
        HULLS.values().forEach(built -> ShipGpu.closeQuietly(built.getGpu()));
        HULLS.clear();
        FAILURES.clear();
        PENDING.values().forEach(pending -> pending.cancel(false));
        PENDING.clear();
        prepareNanos = 0;
        compileNanos = 0;
        meshed = 0;
    }

    public static int pending() {
        return PENDING.size();
    }

    public static int builtHulls() {
        return HULLS.size();
    }

    public static long gpuBytes() {
        long total = 0;
        for (Built built : HULLS.values()) {
            total += built.getGpu().bytes();
        }
        return total;
    }

    public static int sections() {
        int count = 0;
        for (Built built : HULLS.values()) {
            count += built.getSections();
        }
        return count;
    }

    public static int quads() {
        int count = 0;
        for (Built built : HULLS.values()) {
            count += built.getQuads();
        }
        return count;
    }

    public static double meanPrepareMillis() {
        return meshed == 0 ? 0.0 : prepareNanos / 1_000_000.0 / meshed;
    }

    public static double meanCompileMillis() {
        return meshed == 0 ? 0.0 : compileNanos / 1_000_000.0 / meshed;
    }

    public static boolean available() {
        return ShipDrawPass.usable();
    }
}
