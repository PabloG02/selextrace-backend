/*
 *  Copyright 2002-2025, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 */
package pablog.selextrace.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/// The `Accumulator` class is a data type for computing the running
/// mean, sample standard deviation, and sample variance of a stream of real
/// numbers. It provides an example of a mutable data type and a streaming
/// algorithm.
///
/// This implementation uses a one-pass algorithm that is less susceptible
/// to floating-point roundoff error than the more straightforward
/// implementation based on saving the sum of the squares of the numbers.
/// This technique is due to
/// [B. P. Welford](https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm).
/// Each operation takes constant time in the worst case.
/// The amount of memory is constant — the data values are not stored.
///
/// For additional documentation, see
/// [Section 1.2](http://algs4.cs.princeton.edu/12oop) of
/// *Algorithms, 4th Edition* by Robert Sedgewick and Kevin Wayne.
///
/// @author Robert Sedgewick
/// @author Kevin Wayne
public class Accumulator implements Serializable {
    @Serial
    private static final long serialVersionUID = 2243113298802820956L;

    @JsonProperty
    private int n = 0; // number of data values
    @JsonProperty
    private double sum = 0.0; // sample variance * (n-1)
    @JsonProperty
    private double mu = 0.0; // sample mean

    /// Initializes an accumulator.
    public Accumulator() {
    }

    /// Adds the specified data value to the accumulator.
    ///
    /// @param x the data value
    public synchronized void addDataValue(double x) {
        n++;
        double delta = x - mu;
        mu += delta / n;
        sum += (double) (n - 1) / n * delta * delta;
    }

    /// Returns the sample mean of all added data values.
    public double mean() {
        return mu;
    }

    /// Returns the sample variance, or `NaN` if fewer than two data values have been added.
    public double var() {
        if (n <= 1)
            return Double.NaN;
        return sum / (n - 1);
    }

    /// Returns the sample standard deviation, or `NaN` if fewer than two data values have been added.
    public double stddev() {
        return Math.sqrt(this.var());
    }

    /// Returns the number of data values added so far.
    public int count() {
        return n;
    }

}
