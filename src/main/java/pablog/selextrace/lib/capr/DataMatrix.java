package pablog.selextrace.lib.capr;

import java.util.ArrayList;
import java.util.Collections;

/// This class implements a two-dimensional bit array based on a one-dimensional [ArrayList].
/// The indices are transparently translated from 2D to 1D. This allows for simple
/// storage of generic data.
///
/// @param <T> the type of elements in this matrix
/// @author Jan Hoinka
public class DataMatrix<T>{

    /// The list storing the matrix data
    private ArrayList<T> data = null;

    /// The number of rows of the matrix
    private int rows = 0;

    /// The number of columns of the matrix
    private int cols = 0;

    /// Initialize the Matrix in a "row major" manner.
    ///
    /// @param rows number of rows
    /// @param cols number of columns
    /// @throws IllegalArgumentException if rows or cols are <= 0
    public DataMatrix(int rows, int cols){
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("The number of rows (%d) or columns (%d) is invalid".formatted(rows, cols));
        }

        this.rows = rows;
        this.cols = cols;
        this.data = new ArrayList<>(rows * cols);
    }

    /// Initialize an empty matrix (0x0).
    public DataMatrix() {
        this.data = new ArrayList<>();
        this.rows = 0;
        this.cols = 0;
    }

    /// Sets the cell corresponding to (`row`, `col`) of the matrix to `value`.
    ///
    /// @param row   the row index
    /// @param col   the column index
    /// @param value the value to store
    /// @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
    public void set(int row, int col, T value) {
        data.set(cols * row + col, value);
    }

    /// Gets the values of the cell corresponding to (`row`, `col`).
    ///
    /// @param row the row index
    /// @param col the column index
    /// @return the value at the specified coordinates
    /// @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
    public T get(int row, int col) {
        return data.get(cols * row + col);
    }

    /// Resets all values of the matrix to `defaultValue`.
    ///
    /// @param defaultValue the value to fill the matrix with
    public void clear(T defaultValue) {
        Collections.fill(data, defaultValue);
    }

    /// Changes the dimension of the matrix and allocates more space if required.
    /// Note: This effectively invalidates current index mappings if the column count changes.
    ///
    /// @param rows new number of rows
    /// @param cols new number of columns
    public DataMatrix<T> reshape(int rows, int cols) {
        int targetSize = rows * cols;

        // Efficiently grow the list if needed
        if (data.size() < targetSize) {
            data.addAll(Collections.nCopies(targetSize - data.size(), null));
        }

        this.rows = rows;
        this.cols = cols;

        return this;
    }

}
