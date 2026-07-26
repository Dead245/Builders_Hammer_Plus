package buildershammer.Helpers;

import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nonnull;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.protocol.BlockFace;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class RotationFunctions {
      private RotationFunctions() {
        /* This utility class should not be instantiated */
      }

      //Rotate Block Facing
      public static int rotateBlockFacing(@Nonnull InteractionType intType, @Nonnull World world, @Nonnull Vector3i blockPos, String face) {
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

            BlockFace faceDir = BlockFaceCheck.getFace(face);
        
            // Need all this to find the RotationTuple of the block
            WorldChunk targetChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
            if (targetChunk == null) return -1;

            ChunkStore chunkStore = world.getChunkStore();
            Store<ChunkStore> chunkStoreStore = chunkStore.getStore();

            int chunkX = ChunkUtil.chunkCoordinate(blockPos.x);
            int chunkY = ChunkUtil.chunkCoordinate(blockPos.y);
            int chunkZ = ChunkUtil.chunkCoordinate(blockPos.z);

            Ref<ChunkStore> chunkSectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);
            if (chunkSectionRef == null) return -1;

            BlockSection blockSection = chunkStoreStore.getComponent(chunkSectionRef, BlockSection.getComponentType());
            if (blockSection == null) return -1;

            RotationTuple rotation = blockSection.getRotation(blockPos.x, blockPos.y, blockPos.z);

            return nextRotation(rotation, stateDirection, faceDir);
      }

      public static int nextRotation(RotationTuple originalRotation, int direction, BlockFace face) {

            Axis clickedAxis = switch (face) {
                  case BlockFace.Down ->  Axis.Y;
                  case BlockFace.Up -> Axis.Y;
                  case BlockFace.North -> Axis.Z;
                  case BlockFace.South -> Axis.Z;
                  case BlockFace.West -> Axis.X;
                  case BlockFace.East -> Axis.X;
                  default -> Axis.Y;
            };

            RotationTuple newRotTuple = originalRotation.composeOnAxis(clickedAxis,direction == 1 ? Rotation.Ninety : Rotation.TwoSeventy);

            return newRotTuple.index();
      }
}
