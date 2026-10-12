package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions")
public class LithiumCollisionMixin {

    @Inject(method = "appendEntityCollisions", at = @At("TAIL"), require = 0, remap = false)
    private static void nexusveil$appendHull(List<VoxelShape> out, Level level, Entity entity, AABB box,
                                             CallbackInfo callback) {
        out.addAll(ShipCollision.shapesFor(entity, box, ShipCollision.Site.LITHIUM));
    }

    @Inject(method = "doesBoxCollideWithHardEntities", at = @At("RETURN"), cancellable = true,
            require = 0, remap = false)
    private static void nexusveil$hullIsHard(EntityGetter level, Entity entity, AABB box,
                                             CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ()
                && !ShipCollision.shapesFor(entity, box, ShipCollision.Site.LITHIUM).isEmpty()) {
            callback.setReturnValue(true);
        }
    }
}
