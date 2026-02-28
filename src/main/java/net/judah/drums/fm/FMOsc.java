package net.judah.drums.fm;

import java.awt.Point;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.sound.midi.ShortMessage;

import judahzone.data.Letter;
import judahzone.data.Shape;
import judahzone.prism.PrismRT;
import judahzone.prism.PrismUI;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumSetup;
import net.judah.drums.fm.FMSetup.FMOscSetup;
import net.judah.drums.fm.FMSetup.OpSetup;
import net.judah.drums.fm.Operator.Params;
import net.judah.midi.Actives;

@PrismRT public class FMOsc extends Drum implements Supplier<FMOscSetup> {

    @Getter private final int opCount;
    @Getter private final Operator[] ops;
    @Getter private volatile Algorithm algo;

    private final float[] tempOut;
    private final boolean[] isCarrier;
    private final float[] previousOutputs;
    private float lastFundamental = -1f;

    public FMOsc(DrumSetup std, FMOscSetup setup, Actives actives) {
        super(std, actives);
        this.ops = new Operator[setup.operators().length];
        this.opCount = ops.length;
        this.isCarrier = new boolean[opCount];
        this.tempOut = new float[opCount];
        this.previousOutputs = new float[opCount];

        for (int i = 0; i < opCount; i++)
            ops[i] = new Operator(setup.operators()[i], false, ISR);

        algo = setup.algorithm();
        setAlgo(algo);
    }

    private void setFeedback() {
        int[][] mods = algo.modulatorsFor();
        boolean[][] fbFlags = algo.feedbackFlags();

        for (int op = 0; op < opCount; op++) {
            boolean hasExternalSelfLoop = false;
            if (fbFlags != null && op < fbFlags.length) {
                int[] srcs = mods[op];
                for (int j = 0; j < srcs.length; j++) {
                    if (srcs[j] == op && fbFlags[op][j]) {
                        hasExternalSelfLoop = true;
                        break;
                    }
                }
            }
            // If algorithm topology handles a self-loop via previousOutputs, disable Op-internal sync feedback
            ops[op].setSelfFeedback(!hasExternalSelfLoop);
        }
    }

    @Override protected void processImpl() {
        final Algorithm a = this.algo;
        if (a == null) { Arrays.fill(work, 0f); return; }

        boolean active = false;
        for (int i = 0; i < opCount; i++)
            if (ops[i].getEnvelope().isPlaying()) { active = true; break; }

        if (!active) {
            Arrays.fill(previousOutputs, 0f);
            Arrays.fill(work, 0f);
            return;
        }

        float baseHz = getFundamental();
        if (dirty || baseHz != lastFundamental) {
            for (int i = 0; i < opCount; i++)
                ops[i].phaseInc(baseHz);
            lastFundamental = baseHz;
            dirty = false;
        }

        final int[][] mods = a.modulatorsFor();
        final float[][] scales = a.scaleFor();
        final int[] evalOrder = a.evalOrder();
        final boolean[][] feedback = a.feedbackFlags();
        final boolean hasFeedback = feedback != null;

        for (int f = 0; f < work.length; f++) {
            Arrays.fill(tempOut, 0f);

            for (int dst : evalOrder) {
                float modHz = 0f;
                int[] srcs = mods[dst];
                float[] scs = scales[dst];

                for (int j = 0; j < srcs.length; j++) {
                    int src = srcs[j];
                    boolean usePrev = hasFeedback && feedback[dst][j];
                    float modulatorOut = usePrev ? previousOutputs[src] : tempOut[src];
                    modHz += modulatorOut * scs[j] * Constants.TWOPI;
                }

                if (!ops[dst].isSelfFeedback())
                    modHz += (ops[dst].feedbackHz * previousOutputs[dst]);

                ops[dst].generate(tempOut, dst, modHz);
            }

            float mix = 0f;
            for (int i = 0; i < opCount; i++)
                if (isCarrier[i]) mix += tempOut[i];

            work[f] = mix * velocity;
            System.arraycopy(tempOut, 0, previousOutputs, 0, opCount);
        }
    }

    @Override public void trigger(ShortMessage midi) {
        super.trigger(midi);
        lastFundamental = getFundamental();
        for (Operator op : ops) op.trigger(lastFundamental);
        if (midi == null) velocity = 1f;
        Arrays.fill(previousOutputs, 0f);
    }

    @PrismUI public void setAlgo(Algorithm a) {
        this.algo = a;
        if (a == null) return;
        boolean anyCarrier = false;
        for (int i = 0; i < opCount; i++) {
            boolean car = Topology.isCarrier(a, i);
            isCarrier[i] = car;
            if (car) anyCarrier = true;
        }
        if (!anyCarrier) isCarrier[opCount - 1] = true;
        Arrays.fill(previousOutputs, 0f);
        setFeedback();
    }

    public Operator getOp(int i) { return ops[i]; }

    @Override public FMOscSetup get() {
        OpSetup[] opSetups = new OpSetup[ops.length];
        for (int i = 0; i < ops.length; i++) opSetups[i] = ops[i].get();
        return new FMOscSetup(algo, opSetups);
    }

    @Override public void set(int idx, int knob) {
        if (idx == 0) {
            int maxIndex = Math.max(0, DX9.ALGO_COUNT - 1);
            int aIdx = Math.clamp(Math.round(knob * (maxIndex / 100f)), 0, maxIndex);
            setAlgo(DX9.getAlgorithms()[aIdx]);
            return;
        }
        Point coord = getCoord(idx);
        int opIdx = coord.x;
        int paramOrd = coord.y;
        if (opIdx >= opCount || paramOrd >= Params.values().length) return;

        Operator op = ops[opIdx];
        float val = knob / 100f;
        Params param = Params.values()[paramOrd];

        switch (param) {
            case Level: op.setLevel(val); break;
            case Ratio: op.getRatio().setRatioKnob(knob); break;
            case Attack: op.getEnvelope().setAttackMs((long) (val * Letter.MAX_ATTACK_MS)); break;
            case Decay: op.getEnvelope().setDecayMs((long) (val * Letter.MAX_DECAY_MS)); break;
            case Feedback: op.setFeedback(val); break;
            case Shape: op.setShape(Shape.fromKnob(knob)); break;
        }
    }

    private Point getCoord(int idx) {
        if (idx == 0) return new Point(0, 0);
        int opIdx = (idx / 10) - 1;
        int paramOrd = idx % 10;
        return new Point(opIdx, paramOrd);
    }

    @Override public int get(int idx) {
        if (idx == 0) {
            int aIdx = DX9.indexOf(algo);
            return (int) (aIdx / (float)(DX9.ALGO_COUNT - 1) * 100);
        }
        Point coord = getCoord(idx);
        int opIdx = coord.x;
        int paramOrd = coord.y;
        if (opIdx >= opCount || paramOrd >= Params.values().length) return -1;
        Operator op = ops[opIdx];
        Params param = Params.values()[paramOrd];
        float v = 0;
        switch (param) {
            case Level: v = op.getLevel(); break;
            case Ratio: return op.getRatio().getRatioKnob();
            case Attack: v = op.getEnvelope().getAttackMs() / (float) Letter.MAX_ATTACK_MS; break;
            case Decay: v = op.getEnvelope().getDecayMs() / (float) Letter.MAX_DECAY_MS; break;
            case Feedback: v = op.getFeedback(); break;
            case Shape: return Shape.toKnob(op.getShape());
        }
        return (int)(Math.clamp(v, 0, 1) * 100f);
    }

    @Override public String val(int idx) {
        if (idx == 0) {
            int i = DX9.indexOf(algo);
            return i >= 0 ? DX9.names[i] : "???";
        }
        Point coord = getCoord(idx);
        Params p = Params.values()[coord.y];
        if (p == Params.Ratio) return getOp(coord.x).getRatio().toString();
        if (p == Params.Shape) return getOp(coord.x).getShape().name();
        return "" + get(idx);
    }
}
