package net.judah.gui.scope.fft_temp;

//import org.apache.commons.math3.complex.Complex;
//import org.bytedeco.javacpp.DoublePointer;
//
//class PrintHelpers {
//  public static void print(double[] arr) {
//    for (int i = 0; i < arr.length; ++i) {
//      System.out.print(arr[i]);
//      System.out.print(", ");
//    }
//    System.out.println();
//  }
//
//  public static void print_complex_array(Complex[] arr) {
//    for (int i = 0; i < arr.length; i++) {
//      System.out.print(arr[i].toString());
//      System.out.print(", ");
//    }
//    System.out.println();
//  }
//
//  public static void print_double_pointer(DoublePointer ptr, int length) {
//    for (int i = 0; i < length; ++i) {
//      System.out.print(ptr.get(i));
//      System.out.print(", ");
//    }
//    System.out.println();
//  }
//};

public class ConvolutionExample {
//  // This function computes the discrete convolution of two arrays:
//  // result[i] = a[i]*b[0] + a[i-1]*b[1] + ... + a[0]*b[i]
//  // a and b can be vectors of different lengths, this function is careful to never
//  // exceed the bounds of the vectors.
//  private static double[] convolve(double[] a, double[] b) {
//    int n_a = a.length;
//    int n_b = b.length;
//    double[] result = new double[n_a + n_b - 1];
//
//    for (int i = 0; i < n_a + n_b - 1; i++) {
//      double sum = 0.0;
//      for (int j = 0; j <= i; j++) {
//        sum += ((j < n_a) && (i - j < n_b)) ? a[j] * b[i - j] : 0.0;
//      }
//      result[i] = sum;
//    }
//    return result;
//  }
//
//  private static double[] vector_elementwise_multiply(double[] a, double[] b) {
//    assert (a.length == b.length);
//    double[] result = new double[a.length];
//    for (int i = 0; i < result.length; i++) {
//      result[i] = a[i] * b[i];
//    }
//    return result;
//  }
//
//  private static double[] elementwise_complex_multiply(double[] a, double[] b) {
//    assert (a.length == b.length);
//    double[] result = new double[a.length];
//    for (int i = 0; i < result.length; i += 2) {
//      result[i] = a[i] * b[i] - a[i + 1] * b[i + 1];
//      result[i + 1] = a[i] * b[i + 1] + a[i + 1] * b[i];
//    }
//    return result;
//  }
//
//  private static Complex[] elementwise_complex_multiply(Complex[] a, Complex[] b) {
//    assert (a.length == b.length);
//    Complex[] result = new Complex[a.length];
//    for (int i = 0; i < result.length; i++) {
//      result[i] = a[i].multiply(b[i]);
//    }
//    return result;
//  }
//
//  // Convolution of real vectors using the Fast Fourier Transform and the convolution theorem.
//  // See
//  // http://en.wikipedia.org/w/index.php?title=Convolution&oldid=630841165#Fast_convolution_algorithms
//  private static double[] fftw_convolve(double[] a, double[] b) {
//    // Recall that element-wise
//    int padded_length = a.length + b.length - 1;
//
//    // Compute Fourier transform of vector a
//
//    FFTWWrappers.FFTW_R2C_1D_Executor fft_a = new FFTWWrappers.FFTW_R2C_1D_Executor(padded_length);
//    fft_a.set_input_zeropadded(a);
//
//    System.out.print("a: ");
//    PrintHelpers.print_double_pointer(fft_a.get_input_pointer(), fft_a.input_size);
//
//    fft_a.execute();
//
//    System.out.print("FFT(a): ");
//    PrintHelpers.print(fft_a.get_output());
//    System.out.println();
//
//    // Compute Fourier transform of vector b
//
//    FFTWWrappers.FFTW_R2C_1D_Executor fft_b = new FFTWWrappers.FFTW_R2C_1D_Executor(padded_length);
//    fft_b.set_input_zeropadded(b);
//
//    System.out.print("b: ");
//    PrintHelpers.print_double_pointer(fft_b.get_input_pointer(), fft_b.input_size);
//
//    fft_b.execute();
//
//    System.out.print("FFT(b): ");
//    PrintHelpers.print(fft_b.get_output());
//    System.out.println();
//
//    // Perform element-wise product of FFT(a) and FFT(b)
//    // then compute inverse fourier transform.
//    FFTWWrappers.FFTW_C2R_1D_Executor ifft = new FFTWWrappers.FFTW_C2R_1D_Executor(padded_length);
//    assert (ifft.input_size == fft_a.output_size);
//
//    System.out.print("FFT(a) * FFT(b): ");
//    PrintHelpers.print(elementwise_complex_multiply(fft_a.get_output(), fft_b.get_output()));
//    System.out.println();
//
//    ifft.set_input(elementwise_complex_multiply(fft_a.get_output(), fft_b.get_output()));
//    // Alternatively, using the Complex type:
//    // ifft.set_input(elementwise_complex_multiply(fft_a.get_output_as_complex_array(),
//
//    ifft.execute();
//
//    // FFTW returns unnormalized output. To normalize it one must divide each element
//    // of the result by the number of elements.
//    assert (ifft.output_size == padded_length);
//    double[] result = ifft.get_output();
//    for (int i = 0; i < result.length; i++) {
//      result[i] /= padded_length;
//    }
//
//    return result;
//  }
//
//  /**
//   * Main entry point
//   *
//   * @param args
//   */
//  public static void main(String[] args) {
//    double[] a = {2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
//    System.out.println("First vector (a): ");
//    PrintHelpers.print(a);
//
//    double[] b = {1.0, 0.0, 7.0};
//    System.out.println("Second vector (b): ");
//    PrintHelpers.print(b);
//
//    System.out.println("==== Naive convolution ===========================================");
//
//    double[] result_naive = ConvolutionExample.convolve(a, b);
//    System.out.println("Naive convolution result:");
//    PrintHelpers.print(result_naive);
//
//    System.out.println("==== FFT convolution =============================================");
//
//    double[] result_fft = ConvolutionExample.fftw_convolve(a, b);
//    System.out.println("FFT convolution result:");
//    PrintHelpers.print(result_fft);
//  }
}
