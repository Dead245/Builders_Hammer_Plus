package buildershammer.Helpers;
import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3i;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
public class BlockFunctions {
    private BlockFunctions() {
        /* This utility class should not be instantiated */
    }

    // Returns a set of all the coordinates that a block occupies, based on its footprint and rotation.
    public static Set<Vector3i> getFootprint(Vector3i root, int blockId, int rotation) {
        Set<Vector3i> coords = new HashSet<>();
        coords.add(root);
        var footprint = FillerBlockUtil.multiCellFootprint(blockId, rotation);
        if (footprint != null) {
            FillerBlockUtil.testFillerBlocks(footprint, (dx, dy, dz) -> {
                coords.add(new Vector3i(root.x + dx, root.y + dy, root.z + dz));
                return true;
            });
        }
        return coords;
    }

    
}
