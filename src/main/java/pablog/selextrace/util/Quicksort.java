package pablog.selextrace.util;

import java.util.BitSet;

/**
 * Customized quicksort with comparator support for primitive arrays.
 */
public class Quicksort {

    public static void sort(int[] values) {
        class StandardQSComparator implements QSComparator {
            @Override
            public int compare(int a, int b) {
                return Integer.compare(a, b);
            }
        }
        quicksort(values, 0, values.length - 1, new StandardQSComparator());
    }

    public static void sort(int[] values, QSComparator c) {
        quicksort(values, 0, values.length - 1, c);
    }

    public static void sort(int[] values, int[] reference) {
        class StandardQSComparator implements QSComparator {
            @Override
            public int compare(int a, int b) {
                return Integer.compare(a, b);
            }
        }
        quicksort(values, reference, 0, values.length - 1, new StandardQSComparator());
    }

    public static void sort(int[] values, int[] reference, QSComparator c) {
        quicksort(values, reference, 0, values.length - 1, c);
    }

    public static void quicksort(int[] numbers, int low, int high, QSComparator c) {
        int i = low;
        int j = high;
        int pivot = numbers[low + (high - low) / 2];

        while (i <= j) {
            while (c.compare(numbers[i], pivot) == -1) {
                i++;
            }
            while (c.compare(numbers[j], pivot) == 1) {
                j--;
            }
            if (i <= j) {
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksort(numbers, low, j, c);
        }
        if (i < high) {
            quicksort(numbers, i, high, c);
        }
    }

    public static void quicksort(int[] numbers, int[] reference, int low, int high, QSComparator c) {
        int i = low;
        int j = high;
        int pivot = reference[low + (high - low) / 2];

        while (i <= j) {
            while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            while (c.compare(reference[j], pivot) == 1) {
                j--;
            }
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksort(numbers, reference, low, j, c);
        }
        if (i < high) {
            quicksort(numbers, reference, i, high, c);
        }
    }

    public static void quicksort(int[] numbers, int[] reference, BitSet bitset, int low, int high, QSComparator c) {
        int i = low;
        int j = high;
        int pivot = reference[low + (high - low) / 2];

        while (i <= j) {
            while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            while (c.compare(reference[j], pivot) == 1) {
                j--;
            }
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                if (bitset.get(i) != bitset.get(j)) {
                    bitset.flip(i);
                    bitset.flip(j);
                }
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksort(numbers, reference, bitset, low, j, c);
        }
        if (i < high) {
            quicksort(numbers, reference, bitset, i, high, c);
        }
    }

    private static void exchange(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static void quicksort(int[] numbers, double[] reference, int low, int high, QSDoubleComparator c) {
        int i = low;
        int j = high;
        double pivot = reference[low + (high - low) / 2];

        while (i <= j) {
            while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            while (c.compare(reference[j], pivot) == 1) {
                j--;
            }
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksort(numbers, reference, low, j, c);
        }
        if (i < high) {
            quicksort(numbers, reference, i, high, c);
        }
    }

    private static void exchange(double[] array, int i, int j) {
        double temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static QSComparator AscendingQSComparator() {
        class StandardQSComparator implements QSComparator {
            @Override
            public int compare(int a, int b) {
                return Integer.compare(a, b);
            }
        }
        return new StandardQSComparator();
    }

    public static QSComparator DescendingQSComparator() {
        class StandardQSComparator implements QSComparator {
            @Override
            public int compare(int a, int b) {
                return Integer.compare(b, a);
            }
        }
        return new StandardQSComparator();
    }
}
