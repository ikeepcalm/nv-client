package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import dev.ua.ikeepcalm.nexusveil.core.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityClimbMixin {

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void nexusveil$hullLadder(CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (callback.getReturnValueZ() || self != Minecraft.getInstance().player || self.isSpectator()) {
            return;
        }
        BlockState state = ShipCollision.stateAt(new Vec3d(self.getX(), self.getY(), self.getZ()));
        if (state != null && state.is(BlockTags.CLIMBABLE)) {
            callback.setReturnValue(true);
        }
    }
}
