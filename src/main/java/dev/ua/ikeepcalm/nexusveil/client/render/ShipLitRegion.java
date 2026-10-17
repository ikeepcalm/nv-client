package dev.ua.ikeepcalm.nexusveil.client.render;

import dev.ua.ikeepcalm.nexusveil.core.voxel.ShipLightField;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

// The hull is lit by its own blocks, not by the sea it is drawn over: the world's light engine has never heard of her lanterns.
public class ShipLitRegion extends RenderSectionRegion {
    private final ShipLightField light;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public ShipLitRegion(ClientLevel level, int minX, int minY, int minZ, SectionCopy[] sections, ShipLightField light,
                         int anchorX, int anchorY, int anchorZ, int sizeX, int sizeY, int sizeZ) {
        super(level, minX, minY, minZ, sections);
        this.light = light;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        int x = pos.getX() - this.anchorX;
        int y = pos.getY() - this.anchorY;
        int z = pos.getZ() - this.anchorZ;
        if (x < -1 || y < -1 || z < -1 || x > this.sizeX || y > this.sizeY || z > this.sizeZ) {
            return layer == LightLayer.SKY ? 15 : 0;
        }
        ShipLightField.Light at = this.light.at(x, y, z);
        return layer == LightLayer.SKY ? at.getSky() : at.getBlock();
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return Math.max(getBrightness(LightLayer.SKY, pos) - darkening, getBrightness(LightLayer.BLOCK, pos));
    }
}
