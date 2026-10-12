package dev.ua.ikeepcalm.nexusveil.client.ship;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCarry;
import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import dev.ua.ikeepcalm.nexusveil.client.net.NvNetworking;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipRenderer;
import dev.ua.ikeepcalm.nexusveil.core.net.NvGeometry;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipAdd;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipMove;
import dev.ua.ikeepcalm.nexusveil.core.net.NvShipRemove;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientFleet {

    private static final Map<Integer, ClientShip> SHIPS = new LinkedHashMap<>();
    private static final GeometryCache CACHE = new GeometryCache();
    private static final GeometryAssembler ASSEMBLER = new GeometryAssembler(CACHE);

    private static long lastMoveTick;
    private static long movesReceived;

    private ClientFleet() {
    }

    public static void add(NvShipAdd identity) {
        ClientShip ship = new ClientShip(identity);
        SHIPS.put(identity.getHandle(), ship);
        NvLog.LOG.info("Ship {} ({}) joined the view: {}x{}x{}, {} solid, digest {}",
                identity.getHandle(), identity.getName(), identity.getSizeX(), identity.getSizeY(),
                identity.getSizeZ(), identity.getSolidCount(), identity.getDigest());

        ship.model(ASSEMBLER.cached(identity));
        // Asked for even on a cache hit: the digest we send is what stops an edited hull drawing stale.
        NvNetworking.requestGeometry(identity.getHandle(), CACHE.digestOf(identity.getDigest()));
    }

    public static void geometry(NvGeometry part) {
        ClientShip ship = SHIPS.get(part.getHandle());
        if (ship == null) {
            ASSEMBLER.forget(part.getHandle());
            return;
        }
        ship.model(ASSEMBLER.accept(ship.identity(), part));
    }

    public static void move(NvShipMove update) {
        lastMoveTick = update.getTick();
        movesReceived++;
        for (NvShipMove.Entry entry : update.getShips()) {
            ClientShip ship = SHIPS.get(entry.getHandle());
            if (ship != null) {
                ship.move(entry, update.getTick());
            }
        }
    }

    public static void remove(NvShipRemove removal) {
        ASSEMBLER.forget(removal.getHandle());
        ShipRenderer.forget(removal.getHandle());
        ClientShip gone = SHIPS.remove(removal.getHandle());
        if (gone != null) {
            NvLog.LOG.info("Ship {} ({}) left the view: {}",
                    removal.getHandle(), gone.identity().getName(), removal.getReason());
        }
    }

    public static void latch() {
        for (ClientShip ship : SHIPS.values()) {
            ship.latch();
        }
    }

    public static ClientShip byHandle(int handle) {
        return SHIPS.get(handle);
    }

    public static Collection<ClientShip> all() {
        return List.copyOf(SHIPS.values());
    }

    public static int size() {
        return SHIPS.size();
    }

    public static long lastMoveTick() {
        return lastMoveTick;
    }

    public static long movesReceived() {
        return movesReceived;
    }

    public static GeometryAssembler assembler() {
        return ASSEMBLER;
    }

    public static int drawable() {
        int count = 0;
        for (ClientShip ship : SHIPS.values()) {
            if (ship.drawable()) {
                count++;
            }
        }
        return count;
    }

    public static void clear() {
        ShipCarry.forget();
        ShipCollision.clearCache();
        ShipRenderer.clear();
        ASSEMBLER.clear();
        SHIPS.clear();
        lastMoveTick = 0;
        movesReceived = 0;
    }
}
