import javax.vecmath.Matrix4d;
import javax.vecmath.SingularMatrixException;

public abstract class MatrixOperations {

    public static void makeAffine(double[] values) {
        Matrix4d matrix = new Matrix4d(values);
        // normalizing matrix
        // (?)
        for (int i = 0; i < 16; i++) {
            values[i] = matrix.getElement(i / 4, i % 4);
        }
    }

    public static double[] matrixToArray(Matrix4d matrix) {
        double[] array = new double[16];
        for (int i = 0; i < 4; i++) {
             for (int j = 0; j < 4; j++) {
                 array[4 * i + j] = matrix.getElement(i, j);
             }
        }
        return array;
    }

    public static Matrix4d invert(Matrix4d matrix) {
        Matrix4d inverted = new Matrix4d(matrix);
        inverted.invert();
        return inverted;
    }

    public static Matrix4d invert(double[] values) {
        return invert(new Matrix4d(values));
    }

    public static double getMatrixElementValue(String text) throws NumberFormatException {
        return Double.parseDouble(text);
    }

    public static boolean isInvertible(double[] values) {
        try {
            new Matrix4d(values).invert();
            return true;
        } catch (SingularMatrixException e) {
            return false;
        }
    }

    public static Matrix4d getIdentityMatrix() {
        Matrix4d matrix = new Matrix4d();
        matrix.setIdentity();
        return matrix;
    }

}
