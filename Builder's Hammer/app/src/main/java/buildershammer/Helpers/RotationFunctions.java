package buildershammer.Helpers;

import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nonnull;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockFace;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class RotationFunctions {
      private RotationFunctions() {
        /* This utility class should not be instantiated */
      }

      public static int rotateBlockFacing(int direction, @Nonnull World world, @Nonnull Vector3i blockPos, @Nonnull Axis clickAxis) {
            RotationTuple rotation = getRotationTuple(world, blockPos);
            if (rotation == null) return -1;

            return nextRotation(rotation, direction, clickAxis);
      }

      public static int nextRotation(@Nonnull RotationTuple originalRotation, int direction, @Nonnull Axis clickAxis) {
            RotationTuple newRotTuple = originalRotation.composeOnAxis(clickAxis, direction == 1 ? Rotation.Ninety : Rotation.TwoSeventy);

            return newRotTuple.index();
      }

      public static Axis getAxisFromFace(@Nonnull BlockFace face) {
            return switch (face) {
                  case BlockFace.Down -> Axis.Y;
                  case BlockFace.Up -> Axis.Y;
                  case BlockFace.North -> Axis.Z;
                  case BlockFace.South -> Axis.Z;
                  case BlockFace.West -> Axis.X;
                  case BlockFace.East -> Axis.X;
                  default -> Axis.Y;
            };
      }

      public static RotationTuple getRotationTuple(@Nonnull World world, @Nonnull Vector3i blockPos) {
            ChunkStore chunkStore = world.getChunkStore();
            Store<ChunkStore> chunkStoreStore = chunkStore.getStore();

            int chunkX = ChunkUtil.chunkCoordinate(blockPos.x);
            int chunkY = ChunkUtil.chunkCoordinate(blockPos.y);
            int chunkZ = ChunkUtil.chunkCoordinate(blockPos.z);
            
            Ref<ChunkStore> chunkSectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);
            if (chunkSectionRef == null) return null;

            BlockSection blockSection = chunkStoreStore.getComponent(chunkSectionRef, BlockSection.getComponentType());
            if (blockSection == null) return null;

            RotationTuple rotation = blockSection.getRotation(blockPos.x, blockPos.y, blockPos.z);
            return rotation;
      }

      public static Vector3i rotateOffset3D(@Nonnull Vector3i offset, @Nonnull Axis axis, int direction) {
            int x = offset.x;
            int y = offset.y;
            int z = offset.z;

            boolean isPositive = (direction == 1);

            if (axis == Axis.Y) {
                  // Horizontal Yaw Rotation
                  return isPositive 
                        ? new Vector3i(z, y, -x)   // 90° Clockwise
                        : new Vector3i(-z, y, x);  // 90° Counter-Clockwise
            } 
            else if (axis == Axis.X) {
                  // Vertical Pitch Rotation
                  return isPositive 
                        ? new Vector3i(x, -z, y)   // 90° Clockwise
                        : new Vector3i(x, z, -y);  // 90° Counter-Clockwise
            } 
            else if (axis == Axis.Z) {
                  return isPositive 
                        ? new Vector3i(-y, x, z)   // 90° Counter-Clockwise
                        : new Vector3i(y, -x, z);  // 90° Clockwise
            }
            
            return new Vector3i(x, y, z);
      }

      // blockPos is the block's origin, rawBlockPos is the position of the block that was clicked
      public static Vector3i getNewMulticellRoot(@Nonnull Vector3i blockPos, @Nonnull BlockPosition rawBlockPos, @Nonnull Axis axis, int direction) {
            Vector3i newRoot = blockPos;
            if (rawBlockPos.x == blockPos.x && rawBlockPos.y == blockPos.y && rawBlockPos.z == blockPos.z) {
                  return newRoot; // No change needed if the clicked block is the root
            }
            
            Vector3i currOffset = new Vector3i(newRoot.x - rawBlockPos.x, newRoot.y - rawBlockPos.y, newRoot.z - rawBlockPos.z);

            Vector3i rotatedOffset = rotateOffset3D(currOffset, axis, direction);

            newRoot = new Vector3i(rawBlockPos.x, rawBlockPos.y, rawBlockPos.z).add(rotatedOffset);
            return newRoot;
      }
}
