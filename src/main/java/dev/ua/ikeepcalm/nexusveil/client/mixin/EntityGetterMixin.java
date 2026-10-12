package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.collision.ShipCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityGetter.class)
public interface EntityGetterMixin {

    @Inject(method = "getEntityCollisions", at = @At("RETURN"), cancellable = true)
    private void nexusveil$addHullShapes(Entity entity, AABB box,
                                         CallbackInfoReturnable<List<VoxelShape>> callback) {
        List<VoxelShape> hull = ShipCollision.shapesFor(entity, box, ShipCollision.Site.QUERY);
        if (hull.isEmpty()) {
            return;
        }
        List<VoxelShape> all = new ArrayList<>(callback.getReturnValue());
        all.addAll(hull);
        callback.setReturnValue(all);
    }
}
