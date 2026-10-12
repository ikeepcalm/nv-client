# NexusVeil Client

Optional Fabric client mod for servers running the [NexusVeil](../NexusVeil) Paper plugin.

A vanilla client is told a ship exists by being shown thousands of `BlockDisplay` entities, stood on
a grid of invisible shulkers, and pushed along twenty times a second by velocity packets. It works,
and it has a ceiling. This mod gives the client the fact those nine workarounds are substituting
for — **this ship exists, and this is where it is** — and lets it render and collide in the hull's
own coordinate frame.

- the hull is drawn as compiled section meshes, so a galleon is tens of draws rather than thousands
  of entity renders
- the deck is the actual blocks, so a ladder is a ladder and a hatch is a hatch
- standing on a moving ship needs no carry packets at all, because the client already moves with it

**You do not need this mod.** Without it everything works exactly as it does today, and this mod on
any other server does nothing at all. The server can withdraw it per player, mid-session, and a
version mismatch silently hands you back to the vanilla path.

## Build

Requires JDK 25, and the plugin's `:core` jar in your local Maven repository:

The jar is written to `build/libs/nexusveil-client-<version>.jar`. Install it into a Minecraft 26.2
Fabric instance with Fabric API >= 0.160.0. `:core` is bundled inside it, so that is the only file
a player needs.

Bumping `:core` means bumping `nexusveil_core_version` in `gradle.properties` **and** republishing
it; the handshake compares `NvProtocol.VERSION`, which is a `:core` constant, so a stale jar here
refuses itself rather than desyncing.
