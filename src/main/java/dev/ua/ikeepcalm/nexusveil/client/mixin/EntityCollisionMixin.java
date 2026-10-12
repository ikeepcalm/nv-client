package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public class EntityCollisionMixin {

    @Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void nexusveil$addHullColliders(Entity entity, Level level, List<VoxelShape> entityShapes,
                                                   AABB box, CallbackInfoReturnable<List<VoxelShape>> callback) {
        List<VoxelShape> hull = ShipCollision.shapesFor(entity, box, ShipCollision.Site.MOVEMENT);
        if (hull.isEmpty()) {
            return;
        }
        List<VoxelShape> all = new ArrayList<>(callback.getReturnValue());
        all.addAll(hull);
        callback.setReturnValue(all);
    }
}
