// java
package net.judah.bridge;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.util.Constants;
import judahzone.util.RTLogger;

/**
 * A JACK-backed AudioOutput that buffers frames in a small lock\-free ring.
 * Call write(...) from any thread; call processToPorts(...) from the JACK callback.
 */
public class JackAudioSink implements AudioOutput, AutoCloseable {
    private final int bufSize = Constants.bufSize();
    private final int slots;
    private final float[][][] frames; // [slot][channel][frame]
    private final AtomicInteger writeIdx = new AtomicInteger(0);
    private final AtomicInteger readIdx = new AtomicInteger(0);

    private final JackClient jackClient;
    private JackPort outL;
    private JackPort outR;
    private final boolean ownsPorts;

    public JackAudioSink(JackClient client, int ringSlots) throws JackException {
        this.jackClient = client;
        this.slots = Math.max(2, ringSlots);
        this.frames = new float[slots][2][bufSize];
        this.ownsPorts = true;
        // register ports now (must be done before activation)
        outL = jackClient.registerPort("jackSink_left", org.jaudiolibs.jnajack.JackPortType.AUDIO,
                org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput);
        outR = jackClient.registerPort("jackSink_right", org.jaudiolibs.jnajack.JackPortType.AUDIO,
                org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput);
    }

    /**
     * Alternative constructor: supply existing ports (no registration/unregistration).
     */
    public JackAudioSink(JackPort leftPort, JackPort rightPort, int ringSlots) {
        this.jackClient = null;
        this.slots = Math.max(2, ringSlots);
        this.frames = new float[slots][2][bufSize];
        this.outL = leftPort;
        this.outR = rightPort;
        this.ownsPorts = false;
    }

    @Override
    public void write(float[] left, float[] right, int nframes) {
        if (left == null || right == null || nframes <= 0) return;
        int slot = writeIdx.getAndIncrement() % slots;
        // if the ring is full, advance read to make room (drop oldest)
        while ((writeIdx.get() - readIdx.get()) > slots) {
            readIdx.incrementAndGet();
        }
        float[] sL = frames[slot][0];
        float[] sR = frames[slot][1];
        // copy only nframes (remaining buffer data won't be read)
        System.arraycopy(left, 0, sL, 0, Math.min(nframes, bufSize));
        System.arraycopy(right, 0, sR, 0, Math.min(nframes, bufSize));
        // if nframes < bufSize, zero tail to avoid leaking previous data
        if (nframes < bufSize) {
            Arrays.fill(sL, nframes, bufSize, 0f);
            Arrays.fill(sR, nframes, bufSize, 0f);
        }
    }

    /**
     * Call inside the JACK process callback. Copies the next available buffered frame
     * to the provided JackPorts' float buffers. If no frame is available, outputs silence.
     *
     * This method is non\-blocking and real\-time safe (only array copies).
     */
    public void processToPorts(JackPort leftPort, JackPort rightPort, int nframes) {
        java.nio.FloatBuffer outLeft = leftPort.getFloatBuffer();
        java.nio.FloatBuffer outRight = rightPort.getFloatBuffer();
        outLeft.rewind();
        outRight.rewind();

        if (readIdx.get() < writeIdx.get()) {
            int slot = readIdx.getAndIncrement() % slots;
            float[] sL = frames[slot][0];
            float[] sR = frames[slot][1];
            int copy = Math.min(nframes, bufSize);

            outLeft.put(sL, 0, copy);
            outRight.put(sR, 0, copy);

            for (int i = copy; i < nframes; i++) {
                outLeft.put(0f);
                outRight.put(0f);
            }
        } else {
            for (int i = 0; i < nframes; i++) {
                outLeft.put(0f);
                outRight.put(0f);
            }
        }
    }
    /**
     * Convenience: if this sink created its own ports, call this in the JACK callback.
     */
    public void process(int nframes) {
        if (!ownsPorts || outL == null || outR == null) return;
        processToPorts(outL, outR, nframes);
    }

    @Override
    public void start() { /* no-op */ }

    @Override
    public void stop() { /* no-op */ }

    @Override
    public void close() {
        try {
            if (ownsPorts && jackClient != null) {
                // best-effort: unregistering may not be supported in this environment
                // if available, call jackClient.unregisterPort(outL) etc.
            }
        } catch (Throwable t) {
            RTLogger.warn(this, t);
        }
    }
}
