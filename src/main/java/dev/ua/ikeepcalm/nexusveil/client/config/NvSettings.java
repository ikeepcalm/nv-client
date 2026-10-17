package dev.ua.ikeepcalm.nexusveil.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class NvSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NvSettings current = new NvSettings();

    private boolean enabled = true;
    private boolean overlay;
    private double heelScale = 4.0;
    private boolean effects = true;
    private boolean impactFrames = true;
    private boolean shake = true;

    public static NvSettings get() {
        return current;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nexusveil-client.json");
    }

    public static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(path), JsonObject.class);
            NvSettings settings = new NvSettings();
            if (json != null) {
                settings.enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
                settings.overlay = json.has("overlay") && json.get("overlay").getAsBoolean();
                settings.effects = !json.has("effects") || json.get("effects").getAsBoolean();
                settings.impactFrames = !json.has("impact-frames") || json.get("impact-frames").getAsBoolean();
                settings.shake = !json.has("shake") || json.get("shake").getAsBoolean();
                if (json.has("heel-scale")) {
                    settings.heelScale = Math.clamp(json.get("heel-scale").getAsDouble(), 0.0, 10.0);
                }
            }
            current = settings;
        } catch (IOException | RuntimeException e) {
            NvLog.LOG.warn("Could not read {}, using defaults: {}", path, e.getMessage());
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", current.enabled);
        json.addProperty("overlay", current.overlay);
        json.addProperty("heel-scale", current.heelScale);
        json.addProperty("effects", current.effects);
        json.addProperty("impact-frames", current.impactFrames);
        json.addProperty("shake", current.shake);
        try {
            Files.writeString(file(), GSON.toJson(json));
        } catch (IOException e) {
            NvLog.LOG.warn("Could not write {}: {}", file(), e.getMessage());
        }
    }
}
