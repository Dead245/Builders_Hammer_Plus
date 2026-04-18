package buildershammer;

public class RotationMap {
    public static final int[][] rotations = {
    {0, 1, 2, 3},      // Down
    {8, 9, 10, 11},    // Up
    {4, 14, 17, 25},   // North
    {5, 15, 18, 26},   // West
    {6, 12, 19, 27},   // South
    {7, 13, 16, 24}    // East
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

    public static int nextRotation(int rotationIndex) {
        int[] rot = getRotation(rotationIndex);

        return rotations[rot[0]][(rot[1] + 1) % 4];
    }

    public static int nextFace(int rotationIndex) {
        int[] rot = getRotation(rotationIndex);

        return rotations[(rot[0] + 1) % 6][rot[1]];
    }
}
