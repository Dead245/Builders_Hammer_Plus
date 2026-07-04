package buildershammer.Helpers;

import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nonnull;

import org.joml.Vector3i;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

public class RotationFunctions {
    private RotationFunctions() {
        /* This utility class should not be instantiated */
    }

    //Rotate Texture/Orientation
    public static int rotateBlockOrientation(@Nonnull InteractionType intType, @Nonnull World world, @Nonnull Vector3i blockPos) {
        int blockIndex = world.getBlock(blockPos);
        BlockType targetBlockType = BlockType.getAssetMap().getAsset(blockIndex);
        if (targetBlockType == null) {
            return 0;
        }

        //Calculate how to rotate the block
        int stateDirection = 0;
        switch (intType) {
          case InteractionType.Primary -> {
                // Cycle Forwards
                stateDirection = 1;
                world.sendMessage(Message.raw("Primary Trigger"));
          }
          case InteractionType.Secondary -> {
                // Cycle Backwards
                stateDirection = -1;
                world.sendMessage(Message.raw("Secondary Trigger"));
          }
          default -> {
                // Cycle forwards if not Primary/Secondary
                stateDirection = 1;
          }
        }

        int rotation = world.getBlockRotationIndex(blockPos.x, blockPos.y, blockPos.z);
        
        return RotationMap.nextFace(rotation, stateDirection);
    }
    //Rotate Block Facing
    public static int rotateBlockFacing(@Nonnull InteractionType intType, @Nonnull World world, @Nonnull Vector3i blockPos) {
        int stateDirection = 0;
        switch (intType) {
          case InteractionType.Primary -> {
                // Cycle Forwards
                stateDirection = 1;
                world.sendMessage(Message.raw("Primary Trigger"));
          }
          case InteractionType.Secondary -> {
                // Cycle Backwards
                stateDirection = -1;
                world.sendMessage(Message.raw("Secondary Trigger"));
          }
          default -> {
                // Cycle forwards if not Primary/Secondary
                stateDirection = 1;
          }
        }

        int rotation = world.getBlockRotationIndex(blockPos.x, blockPos.y, blockPos.z);
        
        return RotationMap.nextRotation(rotation, stateDirection);
    }
}
