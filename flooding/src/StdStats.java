import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/*************************************************************************
 *  Compilation:  javac StdStats.java
 *  Execution:    java StdStats < input.txt
 *
 *  Library of statistical functions.
 *
 *  The test client reads an array of real numbers from standard
 *  input, and computes the minimum, mean, maximum, and
 *  standard deviation.
 *
 *  The functions all throw a NullPointerException if the array
 *  passed in is null.

 *  % more tiny.txt
 *  5
 *  3.0 1.0 2.0 5.0 4.0
 *
 *  % java StdStats < tiny.txt
 *         min   1.000
 *        mean   3.000
 *         max   5.000
 *     std dev   1.581
 *
 *************************************************************************/

/**
 *  <i>Standard statistics</i>. This class provides methods for computing
 *  statistics such as min, max, mean, sample standard deviation, and
 *  sample variance.
 *  <p>
 *  For additional documentation, see
 *  <a href="http://introcs.cs.princeton.edu/22library">Section 2.2</a> of
 *  <i>Introduction to Programming in Java: An Interdisciplinary Approach</i>
 *  by Robert Sedgewick and Kevin Wayne.
 */
public final class StdStats {

    private StdStats() { }

    /**
      * Return maximum value in array, -infinity if no such value.
      */
    public static double max(double[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) max = a[i];
        }
        return max;
    }

    /**
      * Return maximum value in subarray a[lo..hi], -infinity if no such value.
      */
    public static double max(double[] a, int lo, int hi) {
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        double max = Double.NEGATIVE_INFINITY;
        for (int i = lo; i <= hi; i++) {
            if (a[i] > max) max = a[i];
        }
        return max;
    }

   /**
     * Return maximum value of array, Integer.MIN_VALUE if no such value
     */
    public static int max(int[] a) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) max = a[i];
        }
        return max;
    }

   /**
     * Return minimum value in array, +infinity if no such value.
     */
    public static double min(double[] a) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min) min = a[i];
        }
        return min;
    }

    /**
      * Return minimum value in subarray a[lo..hi], +infinity if no such value.
      */
    public static double min(double[] a, int lo, int hi) {
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        double min = Double.POSITIVE_INFINITY;
        for (int i = lo; i <= hi; i++) {
            if (a[i] < min) min = a[i];
        }
        return min;
    }

   /**
     * Return minimum value of array, Integer.MAX_VALUE if no such value
     */
    public static int min(int[] a) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min) min = a[i];
        }
        return min;
    }

   /**
     * Return average value in array, NaN if no such value.
     */
    public static double mean(double[] a) {
        if (a.length == 0) return Double.NaN;
        double sum = sum(a);
        return sum / a.length;
    }

   /**
     * Return average value in subarray a[lo..hi], NaN if no such value.
     */
    public static double mean(double[] a, int lo, int hi) {
        int length = hi - lo + 1;
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        if (length == 0) return Double.NaN;
        double sum = sum(a, lo, hi);
        return sum / length;
    }

   /**
     * Return average value in array, NaN if no such value.
     */
    public static double mean(int[] a) {
        if (a.length == 0) return Double.NaN;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum = sum + a[i];
        }
        return sum / a.length;
    }

   /**
     * Return sample variance of array, NaN if no such value.
     */
    public static double var(double[] a) {
        if (a.length == 0) return Double.NaN;
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
    }

   /**
     * Return sample variance of subarray a[lo..hi], NaN if no such value.
     */
    public static double var(double[] a, int lo, int hi) {
        int length = hi - lo + 1;
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        if (length == 0) return Double.NaN;
        double avg = mean(a, lo, hi);
        double sum = 0.0;
        for (int i = lo; i <= hi; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (length - 1);
    }

   /**
     * Return sample variance of array, NaN if no such value.
     */
    public static double var(int[] a) {
        if (a.length == 0) return Double.NaN;
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
    }

   /**
     * Return population variance of array, NaN if no such value.
     */
    public static double varp(double[] a) {
        if (a.length == 0) return Double.NaN;
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / a.length;
    }

   /**
     * Return population variance of subarray a[lo..hi],  NaN if no such value.
     */
    public static double varp(double[] a, int lo, int hi) {
        int length = hi - lo + 1;
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        if (length == 0) return Double.NaN;
        double avg = mean(a, lo, hi);
        double sum = 0.0;
        for (int i = lo; i <= hi; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / length;
    }


   /**
     * Return sample standard deviation of array, NaN if no such value.
     */
    public static double stddev(double[] a) {
        return Math.sqrt(var(a));
    }

   /**
     * Return sample standard deviation of subarray a[lo..hi], NaN if no such value.
     */
    public static double stddev(double[] a, int lo, int hi) {
        return Math.sqrt(var(a, lo, hi));
    }

   /**
     * Return sample standard deviation of array, NaN if no such value.
     */
    public static double stddev(int[] a) {
        return Math.sqrt(var(a));
    }

   /**
     * Return population standard deviation of array, NaN if no such value.
     */
    public static double stddevp(double[] a) {
        return Math.sqrt(varp(a));
    }

   /**
     * Return population standard deviation of subarray a[lo..hi], NaN if no such value.
     */
    public static double stddevp(double[] a, int lo, int hi) {
        return Math.sqrt(varp(a, lo, hi));
    }

   /**
     * Return sum of all values in array.
     */
    public static double sum(double[] a) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

   /**
     * Return sum of all values in subarray a[lo..hi].
     */
    public static double sum(double[] a, int lo, int hi) {
        if (lo < 0 || hi >= a.length || lo > hi)
            throw new RuntimeException("Subarray indices out of bounds");
        double sum = 0.0;
        for (int i = lo; i <= hi; i++) {
            sum += a[i];
        }
        return sum;
    }
    
    public static void arrayInsert(double[] a, int loc, double value){
    	for (int i=a.length-1;i>loc;i--){
    		a[i]=a[i-1];
    	}
    	a[loc]=value;
    }
    
    public static void arrayInsert(int[] a, int loc, int value){
    	for (int i=a.length-1;i>loc;i--){
    		a[i]=a[i-1];
    	}
    	a[loc]=value;
    }

   /**
     * Return the nth smallest value in an array.
     * if n=1, return the smallest
     * if n> the length of the input array, return the the largest value
     */
    public static double nth(double[] a, int n) {
    	if (n > a.length ) {n=a.length;}
    	if (n < 1 ) {n=1;}
    	double[] b = new double[a.length];
    	for (int i=0;i<a.length;i++){
    		b[i]=a[i];
    	}
    	Arrays.sort(b);
    	return b[(n-1)];
    }
    
    /**
     * Return the nth smallest value in an array.
     * if n<1, return the smallest
     * if n> the length of the input array, return the the largest value
     */
    public static int nth(int[] a, int n) {
    	if (n > a.length ) {n=a.length;}
    	if (n < 1 ) {n=1;}
    	int[] b = new int[a.length];
    	for (int i=0;i<a.length;i++){
    		b[i]=a[i];
    	}
    	Arrays.sort(b);
        return b[(n-1)];
    }
    
    /**
     * Return p% percentile value in an array where p between 0 and 1.
     * if n<=0, return the smallest
     * if n>=1  return the the largest value
     */
    public static double percentile(double[] a, double p) {
    	if (p > 1.0 ) {p=1.0;}
    	if (p < 0.0 ) {p=0.0;}
    	int pt = (int) ((double)(a.length) * (p));
    	if (pt <=0) {pt=1;}
        return StdStats.nth(a, pt);
    }
    
    /**
     * Return p% percentile value in an array where p between 0 and 1.
     * if n<=0, return the smallest
     * if n>=1  return the the largest value
     */
    public static double percentile(double[][] a2D, double p) {
    	if (p > 1.0 ) {p=1.0;}
    	if (p < 0.0 ) {p=0.0;}
    	double[] a= new double[a2D.length*a2D[0].length];
    	for (int i=0;i<a2D.length;i++){
    	 	for (int j=0;j<a2D[0].length;j++){
        		a[i*a2D[0].length+j]=a2D[i][j];
        	}
       
    	}
    	int pt = (int) ((double)(a.length) * (p));
    	if (pt <=0) {pt=1;}
        return StdStats.nth(a, pt);
    }
    
    /**
     * Return p% percentile value in an array where p between 0 and 1.
     * if n<=0, return the smallest
     * if n>=1  return the the largest value
     */
    public static int percentile(int[] a, double p) {
    	if (p > 1.0 ) {p=1.0;}
    	if (p < 0.0 ) {p=0.0;}
    	int pt = (int) ((double)(a.length) * (p));
    	if (pt <=0) {pt=1;}
        return StdStats.nth(a, pt);
    }

    /**
     * Return p% percentile value in an array where p between 0 and 1.
     * if n<=0, return the smallest
     * if n>=1  return the the largest value
     */
    public static int percentile(int[][] a2D, double p) {
    	if (p > 1.0 ) {p=1.0;}
    	if (p < 0.0 ) {p=0.0;}
    	int[] a= new int[a2D.length*a2D[0].length];
    	for (int i=0;i<a2D.length;i++){
    	 	for (int j=0;j<a2D[0].length;j++){
        		a[i*a2D[0].length+j]=a2D[i][j];
        	}
       
    	}
    	int pt = (int) ((double)(a.length) * (p));
    	if (pt <=0) {pt=1;}
        return StdStats.nth(a, pt);
    }

    
    /**
     * Return sum of all values in array.
     */
    public static int sum(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

 

   /**
     * Test client.
     * Convert command-line arguments to array of doubles and call various methods.
     */
    public static void main(String[] args) {

    }
}