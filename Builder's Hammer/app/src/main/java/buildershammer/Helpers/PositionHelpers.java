package buildershammer.Helpers;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PositionHelpers {
    private PositionHelpers() {
        /* This utility class should not be instantiated */
    }

    // Could utillize Predicate<Integer> for the filter instead of the filteredBlockIDs in the future if needed for more versatility

    public static List<Vector3i> generateCubePositions(@Nonnull final Vector3i centerPos, final int size, @Nonnull final World world, @Nullable final IntSet filteredBlockIDs){
        final List<Vector3i> positions = new ObjectArrayList<>();

        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                for (int dz = -size; dz <= size; dz++) {
                    Vector3i blockPos = new Vector3i(centerPos.x + dx, centerPos.y + dy, centerPos.z + dz);
                    int blockID = world.getBlock(blockPos);

                    if (blockID == BlockType.EMPTY_ID) {
                        continue;
                    }

                    if (filteredBlockIDs == null || filteredBlockIDs.contains(blockID)) {
                        positions.add(new Vector3i(blockPos));
                    }
                }
            }
        }
        return positions;
    }

    public static Int2ObjectSortedMap<List<Vector3i>> radialPositionSort(@Nonnull List<Vector3i> positions, @Nonnull Vector3i centerPos) {

        Int2ObjectSortedMap<List<Vector3i>> rings = new Int2ObjectAVLTreeMap<>();

        for (Vector3i pos : positions) {

            int dx = pos.x - centerPos.x;
            int dy = pos.y - centerPos.y;
            int dz = pos.z - centerPos.z;
            
            //TODO - Potentially upgrade to where I can choose what type of distance calculation to use
            // Chebyshev distance
            int distance = Math.max(Math.abs(dx),Math.max(Math.abs(dy), Math.abs(dz)));

            rings.computeIfAbsent(distance, d -> new ObjectArrayList<>())
                 .add(pos);
        }

        return rings;
    }
}
