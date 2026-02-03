package pablog.aptasuite.util;

public class ArrayUtils {

    /**
     * Reverses the order of the given array.
     * <p>
     * This method does nothing for a {@code null} input array.
     * </p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(final byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }
}
