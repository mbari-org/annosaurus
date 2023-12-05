/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.annotation.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Brian Schlining
 * @since 2015-06-08T14:23:00
 */
public class NearestNeighbor {

    /**
     * Performs a nearest neighbor search. Returns an array of indices of nearest
     * neighbors
     * @param x An ordered array.
     * @param xi The array of values that you are interpolating too.
     * @param epsilon A delta specifying the acceptable offset between each value in xi
     *                and it's nearest neighbor in x
     * @return An array of indices associating each value in xi with it's nearest neighbor
     *         in x. If the nearest neighbor is more than <i>epsilon</i> away, then -1 is
     *         returned
     */
    public static int[] apply(double[] x, double[] xi, double epsilon) {
        if (x == null) {
            throw new IllegalArgumentException("The x array can not be null");
        }

        if (xi == null) {
            throw new IllegalArgumentException("The xi array can not be null");
        }

        int[] idx = new int[xi.length];
        for (int i = 0; i < xi.length; i++) {
            idx[i] = apply(x, xi[i], epsilon);
        }
        return idx;
    }

    private static int apply(double[] x, double i, double epsilon) {
        int n = Arrays.binarySearch(x, i);
        if (n < 0) {
            int j = -n - 1;

            int lower = j - 1;
            if (lower < 0){
                lower = 0;
            }
            int upper = j;
            if (upper >= x.length) {
                upper = x.length - 1;
            }

            double a = x[lower];
            double b = x[upper];
            double da = Math.abs(a - i);
            double db = Math.abs(b - i);

            if (da <= db && da <= epsilon) {
                n = lower;
            }
            else if (db <= epsilon) {
                n = upper;
            }
            else {
                n = -1;
            }
        }
        return n;
    }

    /**
     * Performs a nearest neighbor search. Returns an array of indices of nearest
     * neighbors
     * @param x An ordered array.
     * @param xi The array of values that you are interpolating too.
     * @param epsilon A delta specifying the acceptable offset between each value in xi
     *                and it's nearest neighbor in x
     * @return An array of indices associating each value in xi with it's nearest neighbor
     *         in x. If the nearest neighbor is more than <i>epsilon</i> away, then -1 is
     *         returned
     */
    public static int[] apply(long[] x, long[] xi, long epsilon) {
        if (x == null) {
            throw new IllegalArgumentException("The x array can not be null");
        }

        if (xi == null) {
            throw new IllegalArgumentException("The xi array can not be null");
        }

        int[] idx = new int[xi.length];
        for (int i = 0; i < xi.length; i++) {
            idx[i] = apply(x, xi[i], epsilon);
        }
        return idx;
    }

    private static int apply(long[] x, long i, long epsilon) {
        int n = Arrays.binarySearch(x, i);
        if (n < 0) {
            int j = -n - 1;

            int lower = j - 1;
            if (lower < 0){
                lower = 0;
            }
            int upper = j;
            if (upper >= x.length) {
                upper = x.length - 1;
            }

            long a = x[lower];
            long b = x[upper];
            long da = Math.abs(a - i);
            long db = Math.abs(b - i);

            if (da <= db && da <= epsilon) {
                n = lower;
            }
            else if (db <= epsilon) {
                n = upper;
            }
            else {
                n = -1;
            }
        }
        return n;
    }

    /**
     * Return the values in x at idx.
     * @param x The original data set
     * @param idx The indices into x. This is what is returned by the <i>apply</i> method
     * @param <A> The type of the list
     * @return A subset of x ordered by the values in idx. If idx contains -1 in
     *      an array cell, then null is returned at that index.
     */
    public static <A> List<A> collate(List<A> x, int[] idx) {
        List<A> data = new ArrayList<>();
        for (int i = 0; i < idx.length; i++) {
            int j = idx[i];
            if (j == -1) {
                data.add(null);
            }
            else {
                data.add(x.get(j));
            }
        }
        return data;
    }

}