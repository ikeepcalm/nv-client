package dev.ua.ikeepcalm.nexusveil.client.render;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

// Iris replaces the terrain pipeline the hull is drawn through, so under a shader pack the server's own rig is the hull.
public class Shaders {
    private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
    private static Object api;
    private static Method inUse;
    private static boolean failed;

    private Shaders() {
    }

    public static boolean installed() {
        return IRIS;
    }

    // Reflection, not a compile dependency: most players have no Iris, and the mod must load without it.
    public static boolean active() {
        if (!IRIS || failed) {
            return false;
        }
        try {
            if (inUse == null) {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
            }
            return (boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            NvLog.LOG.warn("Could not ask Iris whether a shader pack is on; hulls are drawn as if none were", e);
            return false;
        }
    }
}
