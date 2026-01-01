package net.judah.gui.fft_temp;

//import org.apache.commons.math3.complex.Complex;
//import org.bytedeco.fftw.global.fftw3;
//import org.bytedeco.javacpp.DoublePointer;

public class FFTWWrappers {
//  public static class FFTW_R2C_1D_Executor {
//    public int input_size;
//    private DoublePointer input_buffer;
//    public int output_size;
//    private DoublePointer output_buffer;
//    private fftw3.fftw_plan plan;
//
//    public FFTW_R2C_1D_Executor(int n_real_samples) {
//      this.input_size = n_real_samples;
//      this.input_buffer = fftw3.fftw_alloc_real(this.input_size);
//      this.output_size = n_real_samples / 2 + 1;
//      this.output_buffer = fftw3.fftw_alloc_complex(this.output_size);
//      this.plan =
//          fftw3.fftw_plan_dft_r2c_1d(
//              this.input_size, this.input_buffer, this.output_buffer, fftw3.FFTW_ESTIMATE);
//    }
//
//    public void free() {
//      fftw3.fftw_destroy_plan(this.plan);
//      fftw3.fftw_free(this.input_buffer);
//      fftw3.fftw_free(this.output_buffer);
//    }
//
//    public void set_input_zeropadded(double[] buffer) {
//      int size = buffer.length;
//      assert (size <= this.input_size);
//      // The DoublePointer type allows for C style memset
//      // and memcpy calls, which were used in the C++
//      // example.  Preserving this logic would seem to require
//      // first creating a DoublePointer from the Java array
//      // (buffer), which involves an additional copy operation.
//      // An alternative might be to use
//      // this.input_buffer.put(buffer) instead of memcpy,
//      // which would still require a memset call or similar.
//      // We use the memcpy approach to more closely follow
//      // the original example.
//      DoublePointer.memcpy(
//          this.input_buffer, new DoublePointer(buffer), this.input_buffer.sizeof() * size);
//      DoublePointer.memset(
//          this.input_buffer.getPointer(size),
//          0,
//          this.input_buffer.sizeof() * (this.input_size - size));
//    }
//
//    public void execute() {
//      fftw3.fftw_execute(plan);
//    }
//
//    public DoublePointer get_input_pointer() {
//      return this.input_buffer;
//    }
//
//    public double[] get_output() {
//      // multiply by 2 as this is an array of doubles
//      // and not complex numbers
//      double[] result = new double[2 * this.output_size];
//      this.output_buffer.get(result);
//      return result;
//    }
//
//    public DoublePointer get_output_pointer() {
//      return this.output_buffer;
//    }
//
//    public Complex[] get_output_as_complex_array() {
//      Complex[] result = new Complex[this.output_size];
//      double[] ds = new double[2 * this.output_size];
//      this.output_buffer.get(ds);
//      for (int i = 0; i < result.length; i++) {
//        result[i] = new Complex(ds[2 * i], ds[2 * i + 1]);
//      }
//      return result;
//    }
//  };
//
//  public static class FFTW_C2R_1D_Executor {
//    public int input_size;
//    private DoublePointer input_buffer;
//    public int output_size;
//    private DoublePointer output_buffer;
//    private fftw3.fftw_plan plan;
//
//    public FFTW_C2R_1D_Executor(int n_real_samples) {
//      this.input_size = n_real_samples / 2 + 1;
//      this.input_buffer = fftw3.fftw_alloc_complex(this.input_size);
//      this.output_size = n_real_samples;
//      this.output_buffer = fftw3.fftw_alloc_real(this.output_size);
//      this.plan =
//          fftw3.fftw_plan_dft_c2r_1d(
//              this.output_size, this.input_buffer, this.output_buffer, fftw3.FFTW_ESTIMATE);
//    }
//
//    public void free() {
//      fftw3.fftw_destroy_plan(this.plan);
//      fftw3.fftw_free(this.input_buffer);
//      fftw3.fftw_free(this.output_buffer);
//    }
//
//    public void set_input(DoublePointer ptr, int size) {
//      assert (size == this.input_size);
//      DoublePointer.memcpy(
//          this.input_buffer, ptr, 2 * ptr.sizeof() * size); // 2 for sizeof(complex)/sizeof(double)
//      DoublePointer.memset(
//          this.input_buffer.getPointer(size), 0, ptr.sizeof() * (this.input_size - size));
//    }
//
//    public void set_input(double[] buffer) {
//      assert ((buffer.length / 2) == this.input_size);
//      // Comments above about memcpy also apply here.
//      DoublePointer.memcpy(
//          this.input_buffer, new DoublePointer(buffer), this.input_buffer.sizeof() * buffer.length);
//      DoublePointer.memset(
//          this.input_buffer.getPointer(buffer.length),
//          0,
//          this.input_buffer.sizeof() * (2 * input_size - buffer.length));
//    }
//
//    public void set_input(Complex[] buffer) {
//      assert (buffer.length == this.input_size);
//      double[] buffer_reals = new double[2 * buffer.length];
//      for (int i = 0; i < buffer.length; i++) {
//        buffer_reals[2 * i] = buffer[i].getReal();
//        buffer_reals[2 * i + 1] = buffer[i].getImaginary();
//      }
//      this.set_input(buffer_reals);
//    }
//
//    public void execute() {
//      fftw3.fftw_execute(plan);
//    }
//
//    public DoublePointer get_output_ponter() {
//      return this.output_buffer;
//    }
//
//    public double[] get_output() {
//      double[] result = new double[this.output_size];
//      this.output_buffer.get(result);
//      return result;
//    }
//  };
};
