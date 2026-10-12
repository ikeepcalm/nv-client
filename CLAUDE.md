# NexusVeil Client

Optional Fabric client mod for the NexusVeil Paper plugin: renders and collides with sailing ships
in each hull's own coordinate frame.

---

## Read this before doing anything

The design lives in the **plugin** repository, not here:

| Document                                                                   | What it is                                                                                                                                              |
|:---------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **[`../NexusVeil/docs/CLIENT_MOD.md`](../NexusVeil/docs/CLIENT_MOD.md)**   | Why this exists, what it is and is not, the protocol, rendering, collision, degradation, phases C0-C4, harnesses, risks. **Read it first, every time.** |
| [`../NexusVeil/docs/PLAN.md`](../NexusVeil/docs/PLAN.md) §3                | D-070 to D-075, the closed decisions. Do not re-litigate one without reading its entry.                                                                 |
| [`../NexusVeil/docs/RENDER_BUDGET.md`](../NexusVeil/docs/RENDER_BUDGET.md) | The measured client cost model every rendering number comes from.                                                                                       |

## The rule that matters most

**The wire format is not a document. It is `:core`, compiled into both halves.**

`dev.ua.ikeepcalm.nexusveil.core.net` carries the protocol version, the channel names, the buffer
codec, every payload record and the server's validation envelope. Both the plugin and this mod run
those exact classes out of the same published jar (D-072). So:

- A protocol change is **one edit, in the plugin's `:core`**, then `./gradlew :core:publishToMavenLocal`
  and a version bump here. Never two edits.
- Nothing in this repository re-implements a `:core` type. If something here seems to need a copy
  of one, it needs to be in `:core` instead.
- `NvProtocol.VERSION` is a `:core` constant, so a mod built against an older core carries an older
  number and the handshake refuses it. That is the only thing standing between a stale jar and a
  silently desynced hull.

## The other half is Bukkit, so the wire must be plain

The sending side is a Paper plugin using `player.sendPluginMessage`. It speaks **one ordinary
`ClientboundCustomPayloadPacket` and nothing else**. So no Fabric-only transport feature may be
used on any channel:

- **Never `PayloadTypeRegistry.registerLarge`.** It is a Fabric-to-Fabric protocol that splits a
  payload across several packets and needs the same splitter on the sending side. A channel
  registered that way silently never reaches its handler against a Bukkit server -- no exception,
  no log, nothing. Every clientbound channel was registered that way in the first build and the
  fault was invisible until a live session, because the only thing tested before it needed nothing
  from the client.
- Our own chunking is `NvGeometry.split` / `join`, in `:core`, where both halves run the same code.
  `NvProtocol.GEOMETRY_CHUNK_BYTES` is the agreement.
- The same reasoning rules out Fabric's configuration-phase channels, its `PacketSender` niceties
  and anything else the server cannot mirror. **If Bukkit cannot send it, we do not use it.**

- **A clientbound payload can be dropped before it is sent.** `CraftPlayer.sendPluginMessage`
  silently discards a message whose channel is not yet in the client's `minecraft:register`, and
  Fabric sends that registration on its own schedule. The server holds payloads until the channel
  appears (`ClientTransport`), so nothing here has to retry -- but it is why a channel added on
  this side must also have a receiver registered, since that is what Fabric declares.

The diagnostic that makes this class of fault visible in one command is `/nvclient`'s first two
lines: `hello: sent` with `payloads in: 0` is a transport fault, and is a different thing from
`server: not a NexusVeil server`.

## Build

```
cd ../NexusVeil && ./gradlew :core:publishToMavenLocal   # whenever :core changed
./gradlew build                                          # -> build/libs/nexusveil-client-<version>.jar
```

`:core` is bundled jar-in-jar, so a player installs one file. Minecraft already ships JOML, which is
`:core`'s only dependency.

## Package layout

```
dev.ua.ikeepcalm.nexusveil.client
  NexusVeilClient     the entrypoint, and only that: load settings, register the wire, wire ticks
  NvLog               the mod's one logger. No System.out anywhere
  config/NvSettings   config/nexusveil-client.json. Small on purpose -- what the server decides
                      is the server's, and only per-machine preferences live here
  net/
    NvPayloads        the Minecraft-side envelope: one Type per channel over a raw byte array
    NvNetworking      registration, every receiver, the hello, and `stand()` -- the one way to
                      hand the ships back to the server for the rest of the session
  ship/
    ClientShip        one hull: identity, geometry, and the two transforms a frame interpolates
    ClientFleet       every ship the server has told this client about, by handle
```

## Code style

Follows the plugin's, not the other Mysterria mods':

- **Classes are not `final`.** Records and enums are implicitly final, which is fine.
- **Comments are rare and load-bearing.** Not what the code does -- why it is correct in a way that
  is not locally obvious, and what a well-meaning edit would break. If you cannot name the edit a
  comment protects against, delete it.
- Java 25. Records, sealed types and pattern-matching `switch` are preferred.
- `this.` on instance field access.
- Fail visibly, never silently: a payload that cannot be read disables the mod for the session and
  hands the ships back, rather than logging once a tick forever.

## Invariants worth knowing

- **Nothing is hidden on the server until this client says it can draw it.** `NvNetworking.ready`
  is that promise and it is sent once per hull, after the geometry has arrived and been built.
  Hiding first is how a declined resource pack once left a player staring at open water where a
  galleon was (D-075).
- **Every failure path ends at the vanilla client experience, never at a broken one.** No server
  answer, a protocol mismatch, an unreadable payload, a renderer that throws: all of them call
  `stand()` and the server resumes its rig, ghost floor and carry within a tick.
- **A mod player and a vanilla player stand on the same deck at the same time**, and the server runs
  both paths per player. Anything built here has to be correct for one player aboard, not for the
  ship.
- **The client is never authoritative.** It is told where a ship is. Physics, ownership, containers,
  the economy and combat all stay on the server.

## Sibling repositories

`../coi-client` and `../mystical-weaponry-hud` are the same author's other Fabric mods for the same
Minecraft version. Use them as **working references for 26.2 idiom** -- how a payload is registered,
how world-space geometry is submitted, how a settings screen is built. Do not copy their protocol
style: both agree a JSON format across two repositories by hand because they have no shared module,
and this one deliberately does not.
