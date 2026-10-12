package dev.ua.ikeepcalm.nexusveil.client.ship;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

public class GeometryCache {

    private static final long MAX_BYTES = 256L * 1024 * 1024;

    private final Path root = FabricLoader.getInstance().getGameDir().resolve("nexusveil/hulls");

    public String digestOf(String digest) {
        return has(digest) ? digest : "";
    }

    public boolean has(String digest) {
        return !digest.isEmpty()
                && Files.isRegularFile(file(digest, "palette"))
                && Files.isRegularFile(file(digest, "indices"));
    }

    public byte[] read(String digest, String kind) {
        try {
            return Files.readAllBytes(file(digest, kind));
        } catch (IOException e) {
            return null;
        }
    }

    // Both blobs go to temp names and are moved into place together: a half-written hull reads back as a corrupt model.
    public void write(String digest, byte[] palette, byte[] indices) {
        try {
            Files.createDirectories(this.root);
            Path paletteTmp = file(digest, "palette.tmp");
            Path indicesTmp = file(digest, "indices.tmp");
            Files.write(paletteTmp, palette);
            Files.write(indicesTmp, indices);
            Files.move(paletteTmp, file(digest, "palette"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(indicesTmp, file(digest, "indices"), StandardCopyOption.REPLACE_EXISTING);
            trim();
        } catch (IOException e) {
            NvLog.LOG.warn("Could not cache hull {}: {}", digest, e.getMessage());
        }
    }

    private void trim() throws IOException {
        if (!Files.isDirectory(this.root)) {
            return;
        }
        try (var entries = Files.list(this.root)) {
            List<Path> files = entries.filter(Files::isRegularFile).toList();
            long total = 0;
            for (Path file : files) {
                total += Files.size(file);
            }
            if (total <= MAX_BYTES) {
                return;
            }
            List<Path> oldest = files.stream()
                    .sorted(Comparator.comparingLong(file -> lastModified(file)))
                    .toList();
            for (Path file : oldest) {
                if (total <= MAX_BYTES) {
                    return;
                }
                total -= Files.size(file);
                Files.deleteIfExists(file);
            }
        }
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path file(String digest, String kind) {
        return this.root.resolve(digest + "." + kind);
    }
}
