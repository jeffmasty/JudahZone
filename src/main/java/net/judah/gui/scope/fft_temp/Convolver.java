package net.judah.gui.scope.fft_temp;

import java.security.InvalidParameterException;

/**
 * <code>Convolver</code> is an effect that convolves a signal with a kernal.
 * The kernal can be thought of as the impulse response of an audio filter, or
 * simply as a set of weighting coefficients. <code>Convolver</code> performs
 * brute-force convolution, meaning that it is slow, relatively speaking.
 * However, the algorithm is very straighforward. Each output sample
 * <code>i</code> is calculated by multiplying each kernal value
 * <code>j</code> with the input sample <code>i - j</code> and then summing
 * the resulting values. The output will be
 * <code>kernal.length + signal.length - 1</code> samples long, so the extra
 * samples are stored in an overlap array. The overlap array from the previous
 * signal convolution is added into the beginning of the output array, which
 * results in a output signal without pops.
 *
 * @author Damien Di Fede
 * @see <a href="http://www.dspguide.com/ch6.htm">Convolution</a>
 *
 */
public class Convolver //implements AudioEffect
{
  protected float[] kernal;
  protected float[] outputL;
  protected float[] overlapL;
  protected float[] outputR;
  protected float[] overlapR;
  protected int sigLen;

  /**
   * Constructs a Convolver with the kernal <code>k</code> that expects buffer
   * of length <code>sigLength</code>.
   *
   * @param k
   *          the kernal of the filter
   * @param sigLength
   *          the length of the buffer that will be convolved with the kernal
   */
  public Convolver(float[] k, int sigLength)
  {
    sigLen = sigLength;
    setKernal(k);
  }

  /**
   * Sets the kernal to <code>k</code>. The values in <code>k</code> are
   * copied so it is not possible to alter the kernal after it has been set
   * except by setting it again.
   *
   * @param k
   *          the kernal to use
   */
  public void setKernal(float[] k)
  {
    kernal = new float[k.length];
    System.arraycopy(k, 0, kernal, 0, k.length);
    outputL = new float[sigLen + kernal.length - 1];
    outputR = new float[sigLen + kernal.length - 1];
    overlapL = new float[outputL.length - sigLen];
    overlapR = new float[outputR.length - sigLen];
  }

  public void process(float[] signal)
  {
    if (signal.length != sigLen)
    {
      throw new InvalidParameterException("Convolver.process: signal.length does not equal sigLen, no processing will occurr.");
    }
    // store the overlap from the previous convolution
    System.arraycopy(outputL, signal.length, overlapL, 0, overlapL.length);
    // convolve kernal with signal and put the result in outputL
    for (int i = 0; i < outputL.length; i++)
    {
      outputL[i] = 0;
      for (int j = 0; j < kernal.length; j++)
      {
        if (i - j < 0 || i - j > signal.length) continue;
        outputL[i] += kernal[j] * signal[i - j];
      }
    }
    // copy the result into signal
    System.arraycopy(outputL, 0, signal, 0, signal.length);
    // add the overlap from the previous convolution to the beginning of signal
    for (int i = 0; i < overlapL.length; i++)
    {
      signal[i] += overlapL[i];
    }
  }

  public void process(float[] sigLeft, float[] sigRight)
  {
    if (sigLeft.length != sigLen || sigRight.length != sigLen)
    {
    	throw new InvalidParameterException("Convolver.process: signal.length does not equal sigLen, no processing will occurr.");
    }
    System.arraycopy(outputL, sigLeft.length, overlapL, 0, overlapL.length);
    System.arraycopy(outputR, sigRight.length, overlapR, 0, overlapR.length);
    for (int i = 0; i < outputL.length; i++)
    {
      outputL[i] = 0;
      outputR[i] = 0;
      for (int j = 0; j < kernal.length; j++)
      {
        if (i - j < 0 || i - j >= sigLeft.length) continue;
        outputL[i] += kernal[j] * sigLeft[i - j];
        outputR[i] += kernal[j] * sigRight[i - j];
      }
    }
    System.arraycopy(outputL, 0, sigLeft, 0, sigLeft.length);
    System.arraycopy(outputR, 0, sigRight, 0, sigRight.length);
    for (int i = 0; i < overlapL.length; i++)
    {
      sigLeft[i] += overlapL[i];
      sigRight[i] += overlapR[i];
    }
  }
}