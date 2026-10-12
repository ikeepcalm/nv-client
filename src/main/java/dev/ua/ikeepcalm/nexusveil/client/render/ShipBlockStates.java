package dev.ua.ikeepcalm.nexusveil.client.render;

import dev.ua.ikeepcalm.nexusveil.client.NvLog;
import dev.ua.ikeepcalm.nexusveil.core.voxel.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ShipBlockStates {

    private ShipBlockStates() {
    }

    public static BlockState[] resolve(Palette palette) {
        List<String> entries = palette.entries();
        BlockState[] states = new BlockState[entries.size()];
        HolderLookup<Block> blocks = lookup();
        int unknown = 0;

        for (int id = 0; id < entries.size(); id++) {
            String key = entries.get(id);
            if (id == Palette.EMPTY_ID || key.isEmpty()) {
                states[id] = Blocks.AIR.defaultBlockState();
                continue;
            }
            try {
                states[id] = BlockStateParser.parseForBlock(blocks, key, false).blockState();
            } catch (Exception e) {
                // Unknown blocks are drawn as air: a gap reads as a hole, a missing-model cube as a fault.
                states[id] = Blocks.AIR.defaultBlockState();
                unknown++;
            }
        }
        if (unknown > 0) {
            NvLog.LOG.warn("{} of {} palette entries would not parse and are drawn as air",
                    unknown, entries.size());
        }
        return states;
    }

    private static HolderLookup<Block> lookup() {
        return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BLOCK);
    }
}
