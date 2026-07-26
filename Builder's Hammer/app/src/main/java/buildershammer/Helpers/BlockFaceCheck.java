package buildershammer.Helpers;

import com.hypixel.hytale.protocol.BlockFace;

public class BlockFaceCheck {
    // Hopefully there is a better way to do this in the future, maybe through SimpleBlockInteraction itself?
    public static BlockFace getFace(String face){
        return switch (face) {
                  case "Down"->  BlockFace.Down;
                  case "Up" -> BlockFace.Up;
                  case "North" -> BlockFace.North;
                  case "West" -> BlockFace.West;
                  case "South" -> BlockFace.South;
                  case "East" -> BlockFace.East;
                  default -> BlockFace.Down;
            };
    }

}
