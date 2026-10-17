package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.net.ReplaySnapshot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

// writeCustomSnapshot is Flashback's own empty hook for mods; @Pseudo so the mod still loads without Flashback.
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public abstract class FlashbackSnapshotMixin {

    @Inject(method = "writeCustomSnapshot", at = @At("HEAD"), require = 0, remap = false)
    private void nexusveil$snapshot(Consumer<Packet<? super ClientGamePacketListener>> out, CallbackInfo ci) {
        ReplaySnapshot.write(out);
    }
}
