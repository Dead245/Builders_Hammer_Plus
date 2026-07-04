package buildershammer.Helpers;

public class RotationMap {
    public static final int[][] rotations = {
    {0, 2, 1, 3},      // Down
    {8, 10, 9, 11},    // Up
    {4, 17, 14, 25},   // North
    {5, 18, 15, 26},   // West
    {6, 19, 12, 27},   // South
    {7, 16, 13, 24}    // East
    };

    public static int[] getRotation(int index) {
        for (int face = 0; face < rotations.length; face++) {
            for (int rot = 0; rot < rotations[face].length; rot++) {
                if (rotations[face][rot] == index) {
                    return new int[]{face, rot};
                }
            }
        }
        return new int[]{0, 0}; // Index not found

    }

    public static int getIndex(int face, int rotation) {
        return rotations[face][rotation];
    }

    public static int nextRotation(int rotationIndex, int direction) {
        int[] rot = getRotation(rotationIndex);

        //return rotations[rot[0]][(rot[1] + direction) % 4];
        return rotations[rot[0]][Math.floorMod(rot[1] + direction, 4)];
    }

    public static int nextFace(int rotationIndex, int direction) {
        int[] rot = getRotation(rotationIndex);

        //return rotations[(rot[0] + direction) % 6][rot[1]];
        return rotations[Math.floorMod(rot[0] + direction, 6)][rot[1]];
    }
}
