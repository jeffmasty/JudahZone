package net.judah.mixer.bus;

import lombok.Getter;

public class Chorus {

    private final LFODelayOp lfoDelay;
    private int sampleRate;
    private int nframes;
    @Getter private float depth;
    @Getter private float rate;
    @Getter private float phase;
    @Getter private float feedback;

    public Chorus() {
        lfoDelay = new LFODelayOp();
//        lfoDelay.setRange(0.5f);
    }

    public void initialize(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.nframes = bufferSize;
    }

    public void setDepth(float depth) {
//        lfoDelay.setDelay(depth / 1000);
        this.depth = depth;
    }


//    public void setRate(float rate) {
//        lfoDelay.setRate(rate);
//    }
//
//    public float getRate() {
//        return lfoDelay.getRate();
//    }
//
//    public void setFeedback(float feedback) {
//        lfoDelay.setFeedback(feedback);
//    }
//
//    public float getFeedback() {
//        return lfoDelay.getFeedback();
//    }
//
//    public void setPhase(float phase) {
//        lfoDelay.setPhase(phase);
//    }
//
//    public float getPhase() {
//        return lfoDelay.getPhase();
//    }

//    public void reset(int skipped) {
//        lfoDelay.reset(skipped);
//    }
//
//    public void processReplace(int buffersize, float[][] outputs, float[][] inputs) {
//        lfoDelay.processReplace(buffersize, outputs, inputs);
//    }
//
//    public void processAdd(int buffersize, float[][] outputs, float[][] inputs) {
//        lfoDelay.processAdd(buffersize, outputs, inputs);
//    }


    class LFODelayOp {

    }
}
