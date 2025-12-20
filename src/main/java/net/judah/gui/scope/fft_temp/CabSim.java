package net.judah.gui.scope.fft_temp;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;
import net.judah.fx.Effect;
import net.judah.omni.Recording;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

// https://www.javaspring.net/java_dip/java_dip_understand_convolution/
// http://ptolemy.eecs.berkeley.edu/eecs20/week12/Image53.gif

public class CabSim implements Effect {

	public static final int MAX_BUFFER_SIZE = N_FRAMES * 64; // modest 1.1 sec reverb?
    public static enum Settings { Preset } /* Wet, HighPass */

	@Getter @Setter private boolean active;
	@Override public String getName() { return CabSim.class.getSimpleName(); }
    @Getter private final int paramCount = Settings.values().length;
    private int preset;
    private float wet = 0.5f;
    private int bufferSize;
    private float[] lbuf, rbuf;
    Recording ir;

	@Override public void set(int idx, int value) {

		if (idx == 0)
			loadPreset(value);

		else if (idx == 1) {
			// TODO dry/wet
		}
		// HighPass?
		// LowPass?
		else throw new InvalidParameterException(idx + " = " + value);

	}

	@Override public int get(int idx) {
		if (idx == 0)
			return preset;
		else if (idx == 1)
			return (int) (wet * 100);
		throw new InvalidParameterException("idx = " + idx);
	}

	private void loadPreset(int file) {
		Folders.getIR();

		File stub = new File(Folders.getIR(), "BritAlnico2x12Medium.wav");
		Recording rec = new Recording();

		try {
			int frames = rec.load(stub);
			bufferSize = frames * N_FRAMES;
			// FILE_SIZE:
			if (bufferSize > MAX_BUFFER_SIZE)
				throw new IOException("IR Samples Overflow: " + bufferSize +
						" Max: " + MAX_BUFFER_SIZE + " " + stub.getAbsolutePath());
			RTLogger.log(this, "IR loaded samples: " + bufferSize);

	 		lbuf = rec.getChannel(Constants.LEFT);
	 		rbuf = rec.getChannel(Constants.RIGHT);

	 		if (lbuf.length != bufferSize)
	 			RTLogger.warn(this, lbuf.length + " L vs " + bufferSize);
	 		if (rbuf.length != bufferSize)
	 			RTLogger.warn(this, rbuf.length + " R vs " + bufferSize);

		} catch (IOException e) { RTLogger.warn(this, e); }

	}

	@Override public void process(FloatBuffer left, FloatBuffer right) {
	  }

	/* private */float[] convolve(FloatBuffer line, float[] b) {
    	float[] a = line.array();
        int n_a = a.length;
        int n_b = b.length;
        float[] result = new float[n_a + n_b - 1];

        for (int i = 0; i < n_a + n_b - 1; i++) {
          float sum = 0;
          for (int j = 0; j <= i; j++) {
            sum += ((j < n_a) && (i - j < n_b)) ? a[j] * b[i - j] : 0;
          }
          result[i] = sum;
        }

        return result;

    }

}
