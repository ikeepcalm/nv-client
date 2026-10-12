package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCarry;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerCarryMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void nexusveil$carry(CallbackInfo callback) {
        ShipCarry.tick((LocalPlayer) (Object) this);
    }
}
