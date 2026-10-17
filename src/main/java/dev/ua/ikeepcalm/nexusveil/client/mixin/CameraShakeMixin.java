package dev.ua.ikeepcalm.nexusveil.client.mixin;

import dev.ua.ikeepcalm.nexusveil.client.fx.CameraShake;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// At the end of alignWithEntity rather than update: update builds the frustum from the camera next.
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    public abstract float xRot();

    @Shadow
    public abstract float yRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void nexusveil$shake(float partialTicks, CallbackInfo ci) {
        float yaw = CameraShake.yaw();
        float pitch = CameraShake.pitch();
        if (yaw != 0f || pitch != 0f) {
            setRotation(yRot() + yaw, xRot() + pitch);
        }
    }
}
