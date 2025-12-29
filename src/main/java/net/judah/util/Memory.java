package net.judah.util;

import java.util.concurrent.LinkedBlockingQueue;
import java. util.concurrent.atomic.AtomicBoolean;

public class Memory {

    public static final Memory STEREO = new Memory(WavConstants.STEREO, Constants.bufSize());
    public static final Memory MONO = new Memory(WavConstants.MONO, Constants.bufSize());

    static final int PRELOAD = 4096;
    static final int THRESHOLD = (int)(PRELOAD * 0.9f);
    static final int RELOAD = (int)(PRELOAD * 0.25f);
    static final String ERROR = "DEPLETED";

    private final LinkedBlockingQueue<float[]> memory = new LinkedBlockingQueue<>();
    private final int channelCount;
    private final int bufSize;
    private final AtomicBoolean reloading = new AtomicBoolean(false);

    public Memory(int numChannels, int bufferSize) {
        this.channelCount = numChannels;
        this.bufSize = bufferSize;
        preload(PRELOAD);
    }

    public float[][] getFrame() {
        // Only trigger reload if not already in progress
        if (memory.size() < THRESHOLD && reloading.compareAndSet(false, true)) {
            Threads.execute(() -> {
                try {
                    preload(RELOAD);
                } finally {
                    reloading. set(false);
                }
            });
        }

        try {
            float[][] result = new float[channelCount][];
            for (int idx = 0; idx < channelCount; idx++) {
                float[] ch = memory.poll();
                if (ch == null)
                    throw new InterruptedException(ERROR);
                result[idx] = ch;
            }
            return result;
        } catch (InterruptedException e) {
            RTLogger.warn(this, e);
            return new float[channelCount][bufSize];
        }
    }

    public void catchUp(Recording tape, int length) {
        for (int i = tape.size(); i < length; i++)
            tape.add(getFrame());
    }

    private void preload(final int amount) {
        for (int i = 0; i < amount; i++)
            memory.add(new float[bufSize]);
    }
}
