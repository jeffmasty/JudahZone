package net.judah.sampler;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;

import judahzone.api.Asset;
import judahzone.fx.Gain;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.AudioTools;
import lombok.Getter;
import net.judah.gui.MainFrame;

/** Audio from disk */
public class Sample extends BasicPlayer {

//	protected static final float BOOST = 0.125f;
	@Getter protected final Gain gain = new Gain();

	@Override
	public void play(boolean onOrOff) {
	    playing = onOrOff;
	}

//	/** load preset by name (without .wav) */
//	public Sample(String wavName, Type type) throws Exception {
//	    this(new File(Folders.getSamples(), wavName + ".wav"), type);
//	}
//
//	/** legacy file-based ctor; uses SampleDB cache to avoid duplicate loads */
//	public Sample(File f, Type type) throws Exception {
//	    this.type = type;
//	    this.file = f;
//	    this.recording = SampleDB.get(f); // uses cache / loads if needed
//	}
//
//	/** explicit gain load (keeps behavior for callers that need a custom gain) */
//	public Sample(String wavName, File f, Type oneShot, float gain) throws IOException {
//	    this.type = oneShot;
//	    this.file = f;
//	    this.recording = Recording.loadInternal(f, gain);
//	}

	/** Asset-based constructor: reuse asset.recording if present, otherwise use SampleDB cache */
	public Sample(Asset asset, Type type) throws Exception {
	    this.type = type;
	    this.file = asset.file();
	    if (asset.recording() != null) {
	        this.recording = asset.recording();
	    } else {
	        this.recording = SampleDB.get(asset.file());
	    }
	}

	@Override // see BasicPlayer
	public void process(float[] outLeft, float[] outRight) {
	    if (!playing) return;

	    int frame = tapeCounter.getAndIncrement();
	    if (frame + 1 >= recording.size()) {
	        tapeCounter.set(0);
	        if (type == Type.ONE_SHOT) {
	            playing = false;
	            MainFrame.update(this);
	        }
	    }
	    if (!playing)
	        return;

	    float[][] buf = recording.get(frame);

	    AudioTools.mix(buf[LEFT], env * gain.getLeft() * gain.getGain(), outLeft);
	    AudioTools.mix(buf[RIGHT], env * gain.getRight() * gain.getGain(), outRight);
	}

	public String getName() {
		return file.getName();
	}

}