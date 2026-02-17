package pablog.aptasuite.lib.capr;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/// Original source code was extracted from Vienna RNA package (version 1.8.5)
/// The author of the original code is Dr. Ivo L Hofacker. The author of the C++
/// implementation is Tsukasa Fukunaga. This code represents a Java implementation of
/// `initloops.h` of the CapR package available at
/// <https://github.com/fukunagatsu/CapR>.
///
/// @author Jan Hoinka
public final class InitLoops {

    // Prevent instantiation
    private InitLoops() {}

    public static final int[][][][] int11_37 = new int[8][8][5][5];
    public static final int[][][][][] int21_37 = new int[8][8][5][5][5];
    /// Adding this array in a standard way would exceed Java's 64k code size limit per method.
    /// Hence, we read it from file.
    public static final int[][][][][][] int22_37 = new int[8][8][5][5][5][5];

    static {
        loadIntArray("capR_int11_37.txt", 4, (idx, value) ->
                int11_37[idx[0]][idx[1]][idx[2]][idx[3]] = value);
        loadIntArray("capR_int21_37.txt", 5, (idx, value) ->
                int21_37[idx[0]][idx[1]][idx[2]][idx[3]][idx[4]] = value);
        loadIntArray("capR_int22_37.txt", 6, (idx, value) ->
                int22_37[idx[0]][idx[1]][idx[2]][idx[3]][idx[4]][idx[5]] = value);
    }

    @FunctionalInterface
    private interface IndexSetter {
        void set(int[] indices, int value);
    }

    /// Reads a nested structure of integers from a resource file and populates an array.
    ///
    /// @param resourceName The name of the file in the classpath.
    /// @param dimensions The expected depth of the nested array structure.
    /// @param setter A callback to place the parsed integer into the correct array slot.
    /// @throws UncheckedIOException If the resource cannot be read.
    private static void loadIntArray(String resourceName, int dimensions, IndexSetter setter) {
        try (BufferedInputStream bis = new BufferedInputStream(openResource(resourceName))) {
            int level = -1;
            int[] indices = new int[dimensions];
            String token;
            while ((token = nextToken(bis)) != null) {
                switch (token) {
                    case "{":
                        level++;
                        break;
                    case "}":
                        // Reset current dimension index
                        indices[level] = 0;
                        level--;
                        if (level != -1) {
                            // Increment parent dimension index
                            indices[level]++;
                        }
                        break;
                    default:
                        // It is a number
                        setter.set(indices, Integer.parseInt(token));
                        indices[level]++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading resource: " + resourceName, e);
        }
    }

    private static InputStream openResource(String resourceName) {
        return Objects.requireNonNull(
                InitLoops.class.getClassLoader().getResourceAsStream(resourceName),
                () -> "Resource not found: " + resourceName);
    }

    /// Parses the next relevant token from the [InputStream].
    ///
    /// Valid tokens include:
    /// * Structural delimiters: `{` or `}`
    /// * Integers: Positive or negative whole numbers
    ///
    /// All other characters (whitespace, commas, etc.) are treated as separators and ignored.
    ///
    /// @param bis The buffered input stream to read from.
    /// @return The next token as a String, or `null` if the end of the stream is reached.
    /// @throws IOException If a read error occurs.
    private static String nextToken(BufferedInputStream bis) throws IOException {
        int b;
        while ((b = bis.read()) != -1) {
            char c = (char) b;

            if (c == '{' || c == '}') {
                return String.valueOf(c);
            }

            boolean negative = false;
            if (c == '-') {
                negative = true;
                b = bis.read();
                if (b == -1) {
                    return null;
                }
                c = (char) b;
            }

            if (Character.isDigit(c)) {
                StringBuilder number = new StringBuilder();
                number.append(c);
                while (true) {
                    bis.mark(1);
                    int next = bis.read();
                    if (next == -1) {
                        break;
                    }
                    char nextChar = (char) next;
                    if (Character.isDigit(nextChar)) {
                        number.append(nextChar);
                    } else {
                        bis.reset();
                        break;
                    }
                }
                return negative ? "-" + number : number.toString();
            }
        }
        return null;
    }
}
