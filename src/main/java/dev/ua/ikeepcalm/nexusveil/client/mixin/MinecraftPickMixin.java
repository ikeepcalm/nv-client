package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCarry;
import dev.ua.ikeepcalm.nexusveil.client.render.ShipPick;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftPickMixin {

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void nexusveil$pickHull(float partialTick, CallbackInfo callback) {
        ShipCarry.frame(partialTick);
        ShipPick.update((Minecraft) (Object) this, partialTick);
    }
}
