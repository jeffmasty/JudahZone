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

import net.judah.JudahZone;
import net.judah.looper.Recording;
import net.judah.util.Constants;


public class AudioTools  {

    private static int z;
    private static float[] workArea = new float[Constants._BUFSIZE];

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
    	a.rewind();
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

	public static String prefixClient(String port) {
		return JudahZone.getInstance().getJackclient().getName() + ":" + port;
	}
	
//	public static String portName(String client, String port) {
//		return client + ":" + port;
//	}
    
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

	/** MIX in and out with gain applied to the input*/
	public static void processAdd(FloatBuffer in, FloatBuffer out) {
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++) 
			out.put(out.get(z) + in.get(z));
	}
	
	/** MIX in and out with gain applied to the input*/
	public static void processAdd(FloatBuffer in, float gain, FloatBuffer out) {
		if (1f == gain) {
			processAdd(in, out);
			return;
		}
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++) 
			out.put(out.get(z) + gain * in.get(z));
	}

	/** MIX */
	public static void processAdd(FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++) 
			out[i] += in.get();
	}

	/** plays in repeatedly until out is filled */
	public static void fill(Recording in, Recording out) {
		int inIdx = 0;
		for (int outIdx = 0; outIdx < out.size(); outIdx++) {
			if (inIdx == in.size())
				inIdx = 0;
			out.set(outIdx, in.get(inIdx));
		}
		
	}
	
	public static void processGain(float[] in, float gain) {
		for (z = 0; z < Constants._BUFSIZE; z++)
			in[z] = in[z] * gain;
	}
	
	public static void processMix(float[] in, FloatBuffer out) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < Constants._BUFSIZE; z++) 
			out.put(workArea[z] + in[z]);
	}
	public static void processMix(float[] in, FloatBuffer out, float gain) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < Constants._BUFSIZE; z++) 
			out.put(workArea[z] + in[z] * gain);
	}

	
	public static void processGain(float[] in, float[] out, float vol) {
		for (z = 0; z < Constants._BUFSIZE; z++) 
			out[z] = in[z] * vol;
	}

	public static void processGain(FloatBuffer buffer, float gain) {
		buffer.rewind();
		for (z = 0; z < Constants._BUFSIZE; z++)
			buffer.put(buffer.get(z) * gain);
		
	}


}


