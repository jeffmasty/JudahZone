package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import judahzone.api.Effect;
import judahzone.api.Effect.RTEffect;
import judahzone.util.Constants;

/**
 * Minimal headless channel strip: buffers, RT effect lists and hotswap.
 */
public class StereoBus {

    protected static final int N_FRAMES = Constants.bufSize();
    protected static final int S_RATE = Constants.sampleRate();

    // per-channel working buffers (owned here so GUI, headless and analyzers share the same buffers)
    protected final FloatBuffer left = FloatBuffer.wrap(new float[N_FRAMES]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[N_FRAMES]);

    // RT effects known to the channel
    protected final ArrayList<RTEffect> rt = new ArrayList<>();

    // The list used by the RT thread while processing
    protected ArrayList<RTEffect> active = new ArrayList<>();

    // The list modified by GUI / presets, then swapped in at a safe point
    private final ArrayList<RTEffect> pendingActive = new ArrayList<>();

    // Offline-active effects (not iterated in RT loop)
    private final List<Effect> offline = new ArrayList<>();

    // All effects known to this channel (RT + offline + LFOs etc.)
    protected final List<Effect> effects = new ArrayList<>();

    // fx activate/deactivate flag
    private volatile boolean activeDirty = false;

    protected StereoBus() {
        pendingActive.addAll(active);
    }

    /** Effects ready at creation */
    public StereoBus(Effect... bus) {
        this();
        for (Effect fx : bus) {
            effects.add(fx);
            if (fx instanceof RTEffect hot)
                rt.add(hot);
        }
    }

    /** Provide external access to the channel work buffers for offline analysis/capture */
    public FloatBuffer getLeft() { return left; }
    public FloatBuffer getRight() { return right; }

    /** process active real-time effects on the supplied buffers */
    public void process(FloatBuffer l, FloatBuffer r) {
        hotSwap();
        for (RTEffect fx : active)
                fx.process(l, r);
    }

    // pass gui changes to the rt thread
    protected void hotSwap() {
        if (activeDirty) {
            active.clear();
            active.addAll(pendingActive);
            activeDirty = false;
        }
    }

    /** activate/deactive effect (hotswap gatekeeper) */
    public void toggle(Effect effect) {
        boolean wasOn = isActive(effect);

        // Determine new "on" state
        boolean nowOn;
        if (!wasOn) {
            // turning on
            nowOn = true;
            effect.activate();
        } else {
            // turning off
            nowOn = false;
            effect.reset();
        }

        if (rt.contains(effect)) {
            // RT effect: operate on pendingActive; swap will occur on RT thread
            if (nowOn) {
                if (!pendingActive.contains(effect))
                    pendingActive.add((RTEffect)effect);
            } else {
                pendingActive.remove(effect);
            }
            activeDirty = true;
        } else if (effects.contains(effect)) {
            // offline effect: just track in offline list
            if (nowOn) {
                if (!offline.contains(effect))
                    offline.add(effect);
            } else {
                offline.remove(effect);
            }
        } else {
            throw new InvalidParameterException(effect.toString());
        }
        // gui updates left to subclasses
    }

    public /* final */ void reset() {
        // deactivate everything through the same path as toggle()
        // but we can do it directly to avoid spamming MainFrame.update for each effect

        for (RTEffect rte : rt) {
            if (pendingActive.contains(rte)) {
                rte.reset();
            }
        }
        // turn off RT effects
        pendingActive.clear();
        activeDirty = true;  // RT thread will pick up empty active list

        // turn off offline effects
        for (Effect fx : offline) {
            fx.reset();
        }
        offline.clear();

        // reset all effect internals (regardless of whether they were active)
        for (Effect fx : effects) {
            fx.reset();
        }
        // gui updates left to subclasses
    }

    public void setActive(Effect fx, boolean on) {
        boolean currentlyOn = isActive(fx);
        if (on == currentlyOn) return;
        toggle(fx);
    }

    public boolean isActive(Effect effect) {
        if (rt.contains(effect))
            return pendingActive.contains(effect);
        return offline.contains(effect);
    }

    public List<Effect> listAll() {
    	return new ArrayList<Effect>(effects);
    }

}