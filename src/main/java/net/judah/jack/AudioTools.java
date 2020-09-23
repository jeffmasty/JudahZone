package net.judah.jack;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.List;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;


public class AudioTools  {

    private final static float DEF_SRATE = 44100;
    private final static float DEF_MAX_DELAY = 2;

    private float gain;
    @SuppressWarnings("unused")
	private float samplerate;
    private float delaytime;
    private float maxdelay;


    /** standard play through, put inputs on the output buffer */
    public static void processEcho(List<FloatBuffer> inputs, List<FloatBuffer> outputs) {
        for (int c = 0; c < outputs.size(); c++) {
        	outputs.get(c).put(inputs.get(c));
        }
    }

	public static void processEcho(float[] in, FloatBuffer out) {
		for (int i = 0; i < out.capacity(); i++)
			out.put(in[i]);
	}


    public static void processSilence(FloatBuffer a) {
    	while(a.hasRemaining()) 
    		a.put(0f);
    	a.rewind();
    }
    
	/** put silence on the out buffer (for slave loops) */
    public static void processSilence(List<FloatBuffer> outputs) {
    	for (FloatBuffer out : outputs) {
    		processSilence(out);
    	}
    }

	public static void processSilence(float[][] buf) {
		for (float[] ch : buf) {
			for (int i = 0; i < ch.length; i++)
				ch[i] = 0f;
		}
	}

    
	public static String portName(String client, String port) {
		
		return client + ":" + port;
	}
	

    
    public static float[][] bufToArray(final List<FloatBuffer> buf) {
    	float[][] result = new float[buf.size()][];
    	for (int i = 0; i < buf.size(); i++) {
    		result[i] = bufToArray(buf.get(i));
    	}
    	return result;
    }

    public static void bufToArray(final FloatBuffer buf, float[] result) {
    	for (int i = 0; i < buf.capacity(); i++) {
    		result[i] = buf.get();
    	}
    	buf.rewind();
    }
    
    public static float[] bufToArray(final FloatBuffer buf) {
    	float[] result = new float[buf.capacity()];
    	for (int i = 0; i < buf.capacity(); i++) {
    		result[i] = buf.get();
    	}
    	buf.rewind();
    	return result;
    }

	public static void arrayToBuf(float[][] from, List<FloatBuffer> to) {
		FloatBuffer buf;
		for (int i = 0; i < to.size(); i++) {
			buf = to.get(i);
			buf.rewind();
			for (int j = 0; j < buf.capacity(); j++) {
				buf.put(from[i][j]);
			}
		}
	}

    protected AudioTools() {
        this(DEF_MAX_DELAY);
    }


    public AudioTools(float maxdelay) {
        if (maxdelay > 0) {
            this.maxdelay = maxdelay;
        }
        this.samplerate = DEF_SRATE;
        this.gain = 1;

    }


	public void setDelay(float time) {
        if (time < 0 || time > maxdelay) {
            throw new IllegalArgumentException();
        }
        this.delaytime = time;
    }

    public float getDelay() {
        return this.delaytime;
    }

    public float getMaxDelay() {
        return this.maxdelay;
    }

    public void setGain(float gain) {
        if (gain < 0) {
            throw new IllegalArgumentException();
        }
        this.gain = gain;
    }

    public float getGain() {
        return this.gain;
    }


	public void initialize(float samplerate, int maxBufferSize) {
        if (samplerate < 1) {
            throw new IllegalArgumentException();
        }
        this.samplerate = samplerate;
    }

	
	public static void makeConnecction(JackClient client, JackPort port, String client2, String port2)
			throws JackException {
	
		String target = client2 + ":" + port2;
	
		// connect to an output port?
		String[] ports = Jack.getInstance().getPorts(client, target, JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsOutput));
		if (ports.length == 1) {
			Jack.getInstance().connect(client, target, port.getName());
			return;
		}
		// connect to an input port?
		ports = Jack.getInstance().getPorts(client, target, JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsInput));
		if (ports.length == 1) {
			Jack.getInstance().connect(client, port.getName(), target);
			return;
		}
		throw new JackException("Did not connect ports " + port.getName() + " and " + target);
	}

	public static float[][] silence(int outputChannels, int bufferSize) {
		float[][] result = new float[outputChannels][bufferSize];
		assert result[0][1] == 0f;
		return result;
	}

	/** MIX
	 *  
	 * @param overdub
	 * @param oldLoop
	 */
	
	public static float[][] overdub(float[][] overdub, float[][] oldLoop) {
		float[][] channels = new float[oldLoop.length][];
		float[] in, out, result;
		for (int channel = 0; channel < oldLoop.length; channel++) {
			in = overdub[channel];
			out = oldLoop[channel];
			result = new float[out.length]; 
			for (int x = 0; x < out.length; x++) {
				result[x] = in[x] + out[x];
			}
			channels[channel] = result;
		}
		return channels;
	}
	
	/** MIX */
	public static void processAdd(FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++) 
			out[i] += in.get();
	}


}


